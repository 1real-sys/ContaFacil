# Caffeine Cache — ContaFácil

## O que é

Caffeine é uma biblioteca de cache em memória para Java, escrita por Ben Manes. É o sucessor moderno do Guava Cache e o padrão do Spring Boot desde a versão 2.x.

Usa um algoritmo chamado **TinyLFU** (Tiny Least Frequently Used), que combina:
- **Frequência de acesso** → rastreia quantas vezes cada entrada é lida (Count-Min Sketch)
- **Recência** → se duas entradas têm frequência similar, despeja a mais antiga

Isso dá uma taxa de acerto (hit rate) próxima do ótimo teórico, superando LRU puro na maioria dos workloads.

## Por que Caffeine e não Redis

| | Caffeine | Redis |
|---|---|---|
| Onde roda | Na JVM do próprio servidor | Servidor externo |
| Latência | Nanossegundos (mesmo heap) | Milissegundos (I/O de rede) |
| Configuração | Zero — adicionou a lib, funciona | Precisa instalar e configurar servidor |
| Dados compartilhados | Só na mesma instância | Entre instâncias |
| Persistência | Não (volátil) | Sim (RDB/AOF) |
| Caso de uso | Cache local de dados que mudam pouco | Cache distribuído, sessão, rate limit |

Caffeine é a escolha certa para **cache local** em uma aplicação monolito. Redis é necessário quando você tem múltiplas instâncias e precisa de consistência entre elas. Este projeto é monolito — Caffeine resolve.

## Como funciona o cache no Spring Boot

O Spring Boot provê uma **abstração de cache**. Você anota métodos com `@Cacheable`, `@CacheEvict`, `@CachePut` e o Spring decide qual implementação concreta usar (Caffeine, Redis, Hazelcast, etc.). A troca é feita por uma propriedade:

```properties
spring.cache.type=caffeine
```

A infraestrutura por baixo:

```
Suas anotações  →  CacheInterceptor (AOP)  →  CacheManager  →  CaffeineCache  →  Caffeine
```

O Spring cria um proxy ao redor do bean. Quando o método anotado é chamado de **fora** da classe, o proxy intercepta e executa a lógica de cache antes/depois do método real.

## As anotações — visão geral

| Anotação | O que faz | Quando usar |
|---|---|---|
| `@Cacheable` | Guarda no cache se não tiver; retorna do cache se tiver | Dados de leitura frequente, mesma entrada lida várias vezes |
| `@CacheEvict` | Apaga do cache | Antes/depois de operações de escrita que tornam o cache obsoleto |
| `@CachePut` | Executa o método e SEMPRE atualiza o cache | Quando precisa garantir que o cache reflita o estado mais recente |
| `@Caching` | Agrupa várias anotações no mesmo método | Quando uma operação precisa de múltiplos evicts ou evict + put |

### `@Cacheable`

```java
@Cacheable(value = "contas", key = "#userId")
List<ContaDomain> findByUserId(Long userId);
```

**Fluxo:**

```
1ª chamada:  cache vazio  →  EXECUTA o método  →  guarda  →  retorna
2ª chamada:  cache cheio  →  NÃO executa  →  retorna do cache
```

| Parâmetro | O que faz |
|---|---|
| `value` | Nome da região de cache ("armário") |
| `key` | Expressão SpEL pra gerar a chave ("gaveta") |
| `unless` | Se for verdadeiro, **não cacheia** o resultado. Ex: `unless = "#result.empty"` — não cacheia Optional vazio |
| `condition` | Se for falso, **desabilita o cache** nessa chamada específica |

**Exemplo prático:**

```java
findByUserId(3L)  →  cache["contas"][3] vazio  →  vai ao banco  →  guarda  →  retorna
findByUserId(3L)  →  cache["contas"][3] cheio  →  retorna direto, banco não consultado
findByUserId(5L)  →  cache["contas"][5] vazio  →  vai ao banco  →  guarda  →  retorna
```

### `@CacheEvict`

```java
@CacheEvict(value = "contas", key = "#entity.user.id")
<S extends ContaDomain> S save(S entity);
```

Remove uma entrada do cache. Força a próxima leitura a ir ao banco buscar dado fresco.

**Fluxo:**

```
save(conta)          →  apaga cache["contas"][entity.user.id]
findByUserId(id)     →  cache["contas"][id] vazio  →  vai ao banco  →  guarda atualizado
```

| Parâmetro | O que faz |
|---|---|
| `value` | Mesma região do `@Cacheable` |
| `key` | Chave específica pra apagar |
| `beforeInvocation` | `false` (padrão): apaga **depois** do método executar. Se o método lançar exceção, **não** apaga. `true`: apaga **antes**, mesmo que o método exploda |
| `allEntries` | `false` (padrão): apaga só uma chave. `true`: apaga TUDO na região inteira |

**Exemplo prático:**

```java
// Usuário 3 deposita R$ 100
ContaDomain conta = contaRepository.findByUserId(3L).get(0);  // cacheia contas[3]
conta.setSaldo(conta.getSaldo().add(new BigDecimal("100")));
contaRepository.save(conta);                                    // @CacheEvict → apaga contas[3]
// Próximo findByUserId(3L) vai ao banco — saldo atualizado
```

### `@CachePut`

```java
@CachePut(value = "contas", key = "#result.user.id")
<S extends ContaDomain> S save(S entity);
```

Sempre executa o método e **atualiza** o cache com o resultado. Diferença do `@Cacheable`: não verifica o cache antes — sempre passa pelo método e depois guarda.

```
Antes:  cache["contas"][3] = { saldo: 100 }
save(conta com saldo 200)
Após:   cache["contas"][3] = { saldo: 200 }   ← atualizado imediatamente
```

Útil quando você quer evitar uma ida ao banco na próxima leitura, em vez de evictar e esperar a próxima leitura popular o cache. Mais agressivo que `@CacheEvict`.

### `@Caching`

Agrupa múltiplas anotações de cache no mesmo método.

```java
@Caching(evict = {
    @CacheEvict(value = "faturas", key = "#entity.cartao.id + '-' + #entity.ano + '-' + #entity.mes"),
    @CacheEvict(value = "faturas", key = "#entity.cartao.id")
})
```

Usado quando uma operação precisa limpar múltiplas chaves (ex: entrada específica + lista).

**Exemplo:** Ao pagar uma fatura, precisa limpar:
1. O cache da fatura específica (`cartaoId-ano-mes`) — senão a próxima consulta devolve valorPago velho
2. O cache da lista de faturas do cartão (`cartaoId`) — senão o histórico mostra dados incorretos

`@Caching` aceita `cacheable`, `put` e `evict` — pode misturar os três.

## Analogia visual

```
@Cacheable(value = "contas", key = "#userId")
           ─────────    ─────────
           armário      gaveta

Cache na memória:

contas[1]  →  ContaDomain{ saldo: 1000, user: { id: 1 } }
contas[2]  →  ContaDomain{ saldo: 5000, user: { id: 2 } }
contas[5]  →  ContaDomain{ saldo: 200,  user: { id: 5 } }

@CacheEvict(value = "contas", key = "#entity.user.id")
            mesmo armário      mesma gaveta

Apaga contas[3] quando entidade com user.id=3 for salva.
```

## A chave (key)

A chave é uma expressão **SpEL** (Spring Expression Language). Referencia os parâmetros do método pelo nome.

```java
@Cacheable(value = "contas", key = "#userId")
List<ContaDomain> findByUserId(Long userId);
// chave gerada: "contas::5"  (se userId = 5)

@CacheEvict(value = "contas", key = "#entity.user.id")
<S extends ContaDomain> S save(S entity);
// chave gerada: "contas::3"  (se entity.getUser().getId() = 3)
```

| Expressão | O que acessa |
|---|---|
| `#userId` | Parâmetro `userId` do método |
| `#entity.user.id` | `entity.getUser().getId()` |
| `#entity.cartao.id + '-' + #entity.ano + '-' + #entity.mes` | Concatenação para chave composta |
| `#result` | Valor de retorno do método |
| `#root.args[0]` | Primeiro argumento pela posição |

Se você quer que `@CacheEvict` limpe a mesma chave que `@Cacheable` usou para guardar, a expressão deve produzir o **mesmo valor**.

## A spec Caffeine

```properties
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=300s
```

| Parâmetro | Significado |
|---|---|
| `maximumSize` | Número máximo de entradas no cache. Quando excede, as menos usadas são despejadas (TinyLFU) |
| `expireAfterWrite` | Tempo após a **escrita** — expira mesmo sem uso. Segurança contra dados zumbis |
| `expireAfterAccess` | Tempo após o último **acesso** — cada leitura renova o timer |
| `initialCapacity` | Tamanho inicial do HashMap interno |
| `weakKeys` / `weakValues` / `softValues` | Referências fracas para o GC coletar sob pressão de memória |

`expireAfterWrite` é o mais usado para cache de dados que o banco pode atualizar por fora. `expireAfterAccess` é bom para sessão de usuário.

## Por que cachear no Repository, não no Service

Havia três candidatos: **repository**, **service**, e um **helper** separado.

| Abordagem | Funciona? | Risco |
|---|---|---|
| `@Cacheable` no repository + `@CacheEvict` no `save()` | ✅ **Sim — sempre** | Zero — todo save passa pelo repositório |
| `@Cacheable` no service (método público) | ✅ Sim | Médio — se alguém chamar `contaRepository.findByUserId()` direto, bypassa o cache |
| Componente helper com `salvar()` e `getContaDoUsuario()` | ✅ Sim | Alto — precisa refatorar todos os services e todo `contaRepository.save()` pra usar o helper |

**A razão:** o repository é o **único ponto de entrada e saída dos dados**. Qualquer fluxo que leia a conta passa por `findByUserId()`. Qualquer fluxo que persista passa por `save()`. Não tem como "furar" o cache. Não requer refatoração de service nenhum.

## Regras de ouro para cache com Caffeine

1. **Dado cacheado não pode ter referência lazy viva** — senão `LazyInitializationException` ao acessar de fora da transação. Use `@EntityGraph` ou `JOIN FETCH` para carregar o que precisar.

2. **`Optional.empty()` não pode ser cacheado** — senão o `orElseGet()` que cria um novo registro nunca executará de novo. Use `unless = "#result.empty"`.

3. **`saveAll()` não dispara `@CacheEvict` do `save()`** — internamente chama `this.save()` sem proxy. Se usar `saveAll()`, anote também.

4. **`@CacheEvict(beforeInvocation = false)` (padrão) executa APÓS o método** — se o método lançar exceção e houver rollback, o cache **não** é limpo. Comportamento desejado.

5. **Chave composta sempre com concatenação consistente** — `#entity.cartao.id + '-' + #entity.ano + '-' + #entity.mes`. A mesma expressão deve ser usada no `@Cacheable` e no `@CacheEvict`.

6. **TTL é rede de segurança, não o mecanismo principal** — `@CacheEvict` garante consistência imediata. O `expireAfterWrite` limpa o que vazar.
