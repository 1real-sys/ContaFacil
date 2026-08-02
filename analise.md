# Análise de Aspectos Técnicos — ContaFácil

---

## Cache

**O que existe:** Nada. Zero dependência de cache. Sem `@Cacheable`, sem `spring-boot-starter-cache`, sem Caffeine, sem Redis.

**O que falta:** Consultas repetidas à conta do usuário autenticado — `getContaUsuario()` é chamada em toda operação (lançar compra, pagar fatura, depositar, sacar, TED, etc.) e bate no banco toda vez. Mesmo dentro da mesma request, serviços diferentes chamam `getContaUsuario()` de novo.

**Impacto no portfólio:** Médio

**Sugestão:** Implementar cache local com Caffeine (já built-in no Spring Boot via `spring-boot-starter-cache`) no método `getContaUsuario()`. 10 linhas de configuração + anotação `@Cacheable`. Mostra que você sabe evitar round-trips desnecessários ao banco. Redis seria overkill pra esse projeto.

---

## Mensageria

**O que existe:** Nada. Sem Kafka, sem RabbitMQ, sem dependências de messaging.

**O que falta:** Já discutimos. O sistema é síncrono do início ao fim. Volume e concorrência não justificam mensageria.

**Impacto no portfólio:** Alto (se bem implementado), mas **não é prioritário**

**Sugestão:** Deixar Kafka para depois da refatoração. O que agrega valor real: publicar `TransacaoRealizadaEvent` ao final de `depositar()`, `Ted()`, `Saque()`, `pagarFatura()`. Um consumer simples que loga ou "envia notificação". 2 tópicos, 1 producer, 1 consumer. Suficiente pra portfólio mostrar que entende o conceito.

---

## Observabilidade

**O que existe:**
- Logs via SLF4J com níveis configuráveis por perfil (`application.properties:11-14`)
- Swagger/OpenAPI (`springdoc-openapi`) para documentação de endpoints
- `GlobalExceptionHandler` loga warnings para `ResponseStatusException` e erros para exceções genéricas
- Hibernate SQL log condicional por ambiente

**O que falta:**
- Spring Actuator (`spring-boot-starter-actuator`) — zero endpoints de health check, métricas, info
- Métricas automáticas via Micrometer (JVM, HTTP, DB pool) — não existe
- Tracing distribuído — não existe (desnecessário pra monolito)
- Log estruturado (JSON) — não existe

**Impacto no portfólio:** Alto

**Sugestão:** Adicionar `spring-boot-starter-actuator` + expor `/actuator/health`, `/actuator/metrics`, `/actuator/info`. Em 5 minutos você tem health check, métricas de JVM/HTTP/DB, e info customizada. Custo zero, retorno alto — todo projeto Spring profissional tem Actuator.

---

## Resiliência

**O que existe:**
- `@Transactional` com rollback atômico em todas as operações de escrita
- `@Version` (optimistic locking) em `ContaDomain`, `CartaoDomain`, `FaturaDomain`
- Rate limiting via `bucket4j` nos endpoints de `/auth/login` e `/auth/register`
- `GlobalExceptionHandler` captura `ResponseStatusException` e `Exception` genérica

**O que falta:**
- Circuit breaker — não existe. Se o banco cair, o Spring retorna 500 genérico
- Retry — não existe. Falha transiente no banco = erro pro usuário
- Timeout configurado nas transações — não existe. `@Transactional` sem `timeout`
- Fallback — não existe

**Impacto no portfólio:** Médio

**Sugestão:** Adicionar `spring-boot-starter-aop` + `@Retryable` do Spring Retry nos métodos `@Transactional`. Configurar `@Transactional(timeout = 10)` nos métodos de escrita. Circuit breaker (Resilience4j) seria canhão pra formiga nesse projeto — o Retry já cobre 90% dos cenários reais de falha.

---

## Concorrência

**O que existe:**
- `@Version` em 3 entidades (optimistic locking via JPA)
- `@Transactional` em todas as operações de escrita
- Rate limiting com `ConcurrentHashMap` no `AuthController`
- `SecurityContextHolder` isolado por thread (stateless)

**O que falta:**
- Tratamento de `OptimisticLockException` — não existe. Se dois usuários pagam a mesma fatura ao mesmo tempo, o segundo recebe 500 genérico
- `cancelarCompra()` sem `@Version` — `ComprasCartaoDomain` não tem campo `@Version` (não está na migration V6)
- `lancarCompra()` — lê `limiteUtilizado`, valida, depois salva — entre a leitura e a escrita outra thread pode consumir o limite restante (check-then-act race). Como `CartaoDomain` tem `@Version`, o segundo save lançaria `OptimisticLockException`, mas sem tratamento vira 500
- `encerrarConta()` — deleta conta sem lock pessimista

**Impacto no portfólio:** Alto

**Sugestão:** Tratar `OptimisticLockException` no `GlobalExceptionHandler` retornando 409 Conflict com mensagem amigável. Adicionar `@Version` em `ComprasCartaoDomain`. Isso mostra maturidade em sistemas concorrentes.

---

## Performance de consultas

**O que existe:**
- Índices básicos nas FKs e colunas de busca frequente (`idx_contas_user_id`, `idx_cartoes_conta_id`, `idx_faturas_cartao_id`, `idx_compras_cartao_fatura_id`, `idx_transacoes_conta_data`, `idx_transacoes_conta_destino_id`)
- Constraints CHECK no banco validam dados na origem
- `FetchType.LAZY` nos `@ManyToOne`

**O que falta:**
- N+1 no `FaturaMapper.toResponse()` — carrega compras da fatura (lista) e pra cada compra acessa `compra.getFatura().getId()`. A fatura já está carregada (é o owner), então não dispara query extra. Mas as compras são carregadas via lazy loading — o `CascadeType.ALL` + `orphanRemoval` no mapeamento faz o JPA carregar a coleção. O `toResponse` dispara `fatura.getCompras()` que é LAZY — então no momento do map, o JPA executa um SELECT separado. Isso é comportamento padrão, mas gera 1 query extra por fatura (N+1 leve).
- `listarMeusCartoes()` — filtra `CANCELADO` em Java, não em SQL
- Nenhuma paginação em listas — `historicoFaturas` retorna todas as faturas de todos os tempos
- `encerrarConta()` — múltiplas queries separadas (consulta cartões, consulta faturas do primeiro cartão) que poderiam ser uma só

**Impacto no portfólio:** Médio

**Sugestão:** Adicionar `@EntityGraph` ou `JOIN FETCH` no `FaturaRepository.findByCartaoIdOrderByAnoDescMesDesc()` para carregar compras junto com a fatura. Adicionar paginação no endpoint de extrato (`Pageable`). Isso mostra que você pensa em performance de banco.

---

## Segurança

**O que existe:**
- JWT com `HMAC256`, expiração de 2 horas
- BCrypt para hash de senha
- CORS configurável por variável de ambiente
- Rate limiting (bucket4j) nos endpoints de auth
- OWASP dependency-check no build
- `SecurityFilterChain` com CSRF desabilitado (stateless API), HSTS, frame options
- Senha validada com regex (min 10 chars, 1 número, 1 maiúscula)
- `@Valid` nos DTOs de entrada
- Validação de propriedade: toda operação verifica se o recurso pertence ao usuário logado
- `GlobalExceptionHandler` não vaza stacktrace em produção (retorna `e.getReason()`)

**O que falta:**
- Refresh token — não existe. Token expira em 2h, usuário faz login de novo
- Roles — todo mundo é `ROLE_USER`. Sem distinção admin/cliente
- Log de tentativas de acesso não autorizado existe mas poderia ser mais granular
- HTTPS forçado via HSTS está OK, mas não tem `requiresChannel()` no SecurityConfig

**Impacto no portfólio:** Médio

**Sugestão:** Implementar refresh token (rotação de token). É o item que mais agrega em entrevista — mostra que você entende o ciclo de vida de autenticação além do básico. Roles podem ficar pra depois (sistema single-role por enquanto).

---

## Testes

**O que existe:** 1 teste — `contextLoads()` que só verifica se o contexto Spring sobe.

**O que falta:** Tudo. Não há teste unitário, integração, controller, repository, nem service. Cobertura efetiva: 0%.

**Impacto no portfólio:** Alto

**Sugestão:** Priorizar testes de integração nos fluxos de negócio — não teste unitário de getter/setter. O que mais impressiona em portfólio:

1. **`TransacaoServiceTest`** — teste de integração com `@DataJpaTest` + `@SpringBootTest`: TED entre duas contas, verifica saldos antes/depois, verifica criação das transações, testa rollback em saldo insuficiente
2. **`FaturaServiceTest`** — pagamento de fatura, verifica débito na conta, liberação de limite, criação de transação de auditoria
3. **`ComprasServiceTest`** — lançar compra, cancelar (ABERTA), estornar (PAGA), testar bloqueios (fatura já cancelada, fatura não PAGA para estorno)

Foco em 3 classes, ~15 testes. Mostra teste de fluxo financeiro com validação de estado. Valor muito maior que 100 testes unitários de DTO.

---

## Resumo para priorização

| Ordem | Aspecto | Impacto | Esforço |
|---|---|---|---|
| 1 | Testes | Alto | 2-3h |
| 2 | Observabilidade (Actuator) | Alto | 30min |
| 3 | Concorrência (tratar OptimisticLockException + @Version faltante) | Alto | 1h |
| 4 | Cache (Caffeine) | Médio | 30min |
| 5 | Segurança (refresh token) | Médio | 1-2h |
| 6 | Performance (JOIN FETCH, paginação) | Médio | 1h |
| 7 | Resiliência (@Retryable) | Médio | 30min |
| 8 | Mensageria (Kafka) | Alto | 3-4h |

Os 3 primeiros são o que separa um projeto de tutorial de um projeto profissional.
