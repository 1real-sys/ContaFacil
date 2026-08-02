# Relatório de Segurança — ContaFacil

Data da análise: 25/05/2026

---

## Parte 1 — OWASP Top 10

---

### A01 - Broken Access Control

**Status:** ⚠️ Atenção

**Onde:**
- `src/main/java/dev/teamwin/contafacil/infra/security/SecurityFilter.java:2388-2392`
- `src/main/java/dev/teamwin/contafacil/infra/security/CustomUserDetailsService.java:2350`
- `src/main/java/dev/teamwin/contafacil/user/UserService.java` (classe vazia)

**Problema:** Todos os usuários autenticados recebem `ROLE_USER` hardcoded, sem distinção de permissões. Não há controle de acesso baseado em roles (ex: ADMIN vs USER). O `CustomUserDetailsService` retorna `new ArrayList<>()` (authorities vazias), inconsistente com o `SecurityFilter` que atribui `ROLE_USER`. O `UserService.java` é uma classe completamente vazia, sem qualquer lógica de autorização implementada.

**Sugestão de correção:**
- Implementar roles reais (USER, ADMIN) no banco de dados
- Adicionar `@PreAuthorize` ou regras específicas no `SecurityConfig` para endpoints críticos (`encerrarConta`, `cancelarCartao`)
- Remover a classe `UserService` vazia ou implementá-la
- Unificar a criação de authorities entre `SecurityFilter` e `CustomUserDetailsService`

---

### A02 - Cryptographic Failures    

//A02 Resolvido
**Status:** 🔴 Vulnerabilidade encontrada — **Crítica**

**Onde:**
- `src/main/resources/application.properties:9` — `api.security.token.secret=my-secret-key`
- `src/main/java/dev/teamwin/contafacil/infra/security/TokenService.java:2299-2304`

**Problema:** A chave secreta do JWT é trivial (`my-secret-key`), hardcoded no `application.properties` versionado no Git. A chave tem apenas 13 caracteres ASCII, extremamente fraca para HMAC256. Qualquer pessoa com acesso ao código-fonte (repositório público, ex-funcionário, vazamento) pode forjar tokens JWT válidos, se autenticar como qualquer usuário e acessar todos os endpoints protegidos.

**Sugestão de correção:**
- Remover a chave do `application.properties` e do versionamento
- Usar variável de ambiente (`${JWT_SECRET}`) com fallback apenas em desenvolvimento
- Gerar chave forte de no mínimo 256 bits: `openssl rand -base64 32`
- Adicionar `api.security.token.secret` ao `.gitignore` ou usar configuração externa

---

### A03 - Injection

**Status:** ✅ Sem problemas

Todas as queries ao banco usam Spring Data JPA com `@Query` parametrizadas ou métodos derivados (`findByEmail`, `findByContaCorrente`), que geram `PreparedStatement` contra SQL injection. Nenhuma concatenação de SQL bruto. Os DTOs usam `@Valid` com constraints Jakarta Validation, rejeitando entradas maliciosas na borda.

---

### A04 - Insecure Design

**Status:** ⚠️ Atenção

**Onde:**
- `src/main/java/dev/teamwin/contafacil/service/AuthService.java:2250-2256`
- `src/main/java/dev/teamwin/contafacil/service/AuthService.java:2252`
- `src/main/java/dev/teamwin/contafacil/transacao/TransacaoService.java:1935-1941`
- `src/main/java/dev/teamwin/contafacil/conta/ContaService.java:117`

**Problema:**
1. Login usa `RuntimeException` genérica — a mensagem "Email já cadastrado" aparece quando o email **não** é encontrado (linha 2252), vazando informação sobre existência de usuários //resolvido
2. Sem rate limiting — brute force irrestrito no `/auth/login` //resolvido
3. Sem idempotency tokens — pagamentos podem ser duplicados se o cliente retentar após timeout
4. Bug na TED: `TransacaoService.java:1936` — a transação de destino registra `contaOrigem` como `contaDestino`, potencialmente corrompendo dados de extrato // sem base, teste em produção normal
5. Hard delete em `encerrarConta()` remove permanentemente o histórico de transações

**Sugestão de correção:**
- Padronizar mensagens de erro no login para "Credenciais inválidas" em ambos os casos //resolvido
- Adicionar rate limiting com Bucket4j ou Spring Cloud Gateway //resolvido
- Implementar idempotency key nos endpoints de pagamento
- Corrigir bug: na linha 1936, o segundo parâmetro de `fromTedRequest` deve ser a conta destino correta
- Usar soft delete em vez de hard delete para `encerrarConta()`

---

### A05 - Security Misconfiguration

**Status:** 🔴 Vulnerabilidade encontrada — **Alta**

**Onde:**
- `src/main/java/dev/teamwin/contafacil/infra/security/SecurityConfig.java:2436` — `.cors(Customizer.withDefaults())`
- `src/main/resources/application.properties:3-4` — `root / Java25oop@`
- `src/main/resources/application.properties:6` — `spring.jpa.show-sql=true`
- `src/main/java/dev/teamwin/contafacil/infra/security/SecurityConfig.java:2439-2441` — Swagger público
- `pom.xml` — `spring-boot-devtools` sem restrição de perfil

**Problema:**
1. CORS `withDefaults()` permite qualquer origem — risco de ataques cross-origin //resolvido
2. Senha do banco `root/Java25oop@` commitada em texto plano no repositório //resolvido
3. `show-sql=true` loga todas as queries no console, podendo expor dados sensíveis //resolvido
4. Swagger UI público (`/swagger-ui/**`) expõe toda a superfície de ataque da API //resolvido
5. `devtools` sem restrição de perfil pode causar vazamentos em produção //resolvido
6. Sem headers de segurança HTTP (`X-Content-Type-Options`, `X-Frame-Options`, `HSTS`) //resolvido

**Sugestão de correção:**
- Restringir CORS: `.cors(cors -> cors.configurationSource(corsConfigurationSource()))` com origens específicas //resolvido
- Externalizar credenciais do banco via variáveis de ambiente (`${DB_USERNAME}`, `${DB_PASSWORD}`)  //resolvido
- Configurar `logging.level.org.hibernate.SQL=WARN` por perfil; desabilitar `show-sql` em produção //resolvido
- Proteger Swagger com autenticação ou removê-lo de builds de produção  
- Mover `devtools` para perfil `dev` com `<scope>runtime</scope>` e `<optional>true</optional>` //resolvido
- Adicionar headers de segurança no `SecurityConfig` //resolvido

---

### A06 - Vulnerable and Outdated Components

**Status:** ⚠️ Atenção

**Onde:**
- `pom.xml:10` — Spring Boot `4.0.5`
- `pom.xml:56-60` — `java-jwt:4.5.0`

**Problema:**
1. Spring Boot 4.0.x não é versão estável GA (General Availability). A estável atual é 3.4.x. Versões pré-lançamento podem conter vulnerabilidades não descobertas
2. `java-jwt 4.5.0` — verificar se é versão publicada; a última estável conhecida é 4.4.0
3. Sem plugin `dependency-check-maven` para auditoria automatizada de CVEs //resolvido

**Sugestão de correção:**
- Migrar para Spring Boot 3.4.x (estável) ou 3.3.x LTS //resolvido
- Verificar versão correta do `java-jwt` no Maven Central // resolvido
- Adicionar `owasp-dependency-check-maven` ao `pom.xml` //resolvido
- Executar `mvn dependency-check:check` periodicamente no CI/CD // talvez

---

### A07 - Identification and Authentication Failures

**Status:** 🔴 Vulnerabilidade encontrada — **Alta**

**Onde:**
- `src/main/java/dev/teamwin/contafacil/dto/login/RegisterRequestDTO.java:10` — `@Size(min = 6)`
- `src/main/java/dev/teamwin/contafacil/dto/login/LoginRequestDTO.java:7` — `@NotBlank` sem tamanho mínimo
- `src/main/java/dev/teamwin/contafacil/service/AuthService.java:2250-2256`
- `src/main/java/dev/teamwin/contafacil/user/UserCreateDTO.java:8` — `@Size(min = 8)`

**Problema:**
1. Senhas de apenas 6 caracteres são permitidas no registro — extremamente fracas contra brute force offline  //resolvido
2. `LoginRequestDTO` não exige tamanho mínimo na senha  //resolvido 
3. Sem proteção contra brute force — atacante pode tentar senhas ilimitadas  //resolvido 
4. Inconsistência: `UserCreateDTO` exige min 8, mas `RegisterRequestDTO` exige min 6 //resolvido 
5. Sem requisitos de complexidade (maiúsculas, números, caracteres especiais) //resolvido
6. Mensagens de erro inconsistentes no login podem vazar se email existe ou não //resolvido

**Sugestão de correção:**
- Aumentar tamanho mínimo de senha para 8+ e unificar entre `RegisterRequestDTO` e `UserCreateDTO` //resolvido
- Adicionar regras de complexidade: `@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$")` //resolvido
- Implementar rate limiting no `/auth/login` (máx 5 tentativas/min por IP) //resolvido 
- Usar mensagem genérica "Credenciais inválidas" para ambos os casos de falha no login //resolvido
- Remover ou implementar `UserService` //resolvido

---

### A08 - Software and Data Integrity Failures

**Status:** ✅ Sem problemas

O projeto não faz download de dependências em runtime, não usa CDN pipelines e não executa código externo. Dependências Maven são baixadas do Maven Central com checksums. O JWT é assinado com HMAC256 (embora a chave seja fraca — ver A02). Nenhum risco de integridade identificado.

---

### A09 - Security Logging and Monitoring Failures

//A09 Resolvido
**Status:** 🔴 Vulnerabilidade encontrada — **Alta**

**Onde:**
- Projeto inteiro — ausência de qualquer configuração de logging
- `src/main/java/dev/teamwin/contafacil/infra/exception/GlobalExceptionHandler.java:2502-2503`
- `src/main/resources/application.properties:6` — `show-sql=true` não é logging adequado
- `src/main/java/dev/teamwin/contafacil/infra/security/SecurityFilter.java:2391`

**Problema:**
1. Nenhuma configuração de logging (`logback-spring.xml`, `application.properties`) //resolvido
2. Nenhum log de eventos de segurança: tentativas de login (sucesso/falha), criação de conta, transações financeiras //resolvido
3. `GlobalExceptionHandler` captura exceções sem logar — impossível rastrear erros ou ataques //resolvido
4. `SecurityFilter` cria autenticação sem logar o evento //resolvido
5. Operações financeiras (`depositar`, `sacar`, `TED`, `pagarFatura`, `encerrarConta`) sem qualquer auditoria //resolvido
6. `show-sql=true` não é mecanismo de logging adequado para produção //resolvido

**Sugestão de correção:**
- Adicionar `spring-boot-starter-actuator` e configurar Logback com níveis por perfil //resolvido
- Criar logs de auditoria para: login (sucesso/falha), criação de conta, emissão de cartão, transações financeiras, cancelamentos //resolvido
- Logar no `GlobalExceptionHandler` com `log.warn("Erro: {}", e.getMessage(), e)` //resolvido
- Usar SLF4J com MDC para incluir `userId` e `requestId` nos logs //resolvido
- Desabilitar `show-sql` em produção; usar `logging.level.org.hibernate.SQL=DEBUG` apenas em dev //resolvido

---

### A10 - Server-Side Request Forgery (SSRF)

**Status:** ✅ Sem problemas

O backend não faz requisições HTTP para nenhum serviço externo. Não há endpoints que aceitem URLs como parâmetro. Todo o tráfego é entre o banco MySQL local e os controllers Spring. Nenhum risco de SSRF.

---

## Parte 2 — Além do OWASP Top 10

---

### 1. Rate Limiting

**Status:** 🔴 Problema encontrado — **Alta**

**Onde:** Projeto inteiro — ausência total de mecanismo de rate limiting.

**Problema:** Nenhum endpoint possui rate limiting. Sem Bucket4j, Spring Cloud Gateway, Resilience4j ou filtro customizado. Um atacante pode:
- Fazer brute force no `/auth/login` com milhares de tentativas por segundo //resolvido 
- Criar contas falsas massivamente via `/auth/register` //resolvido
- Sobrecarregar o banco com chamadas repetidas a `/conta/extrato`

**Sugestão de correção:**
- Adicionar dependência `bucket4j-core` ao `pom.xml`
- Criar um `RateLimitInterceptor` ou filtro que bloqueie > 5 req/min no `/auth/login` por IP
- Limitar demais endpoints autenticados a 30-60 req/min
- Usar `spring.cache.type=caffeine` para armazenar contadores de requisição

---

### 2. Regras de negócio exploráveis

**Status:** 🔴 Problema encontrado — **Crítica**

**Onde:**
- `src/main/java/dev/teamwin/contafacil/comprasCartao/ComprasService.java:64-86` — `cancelarCompra()`
- `src/main/java/dev/teamwin/contafacil/fatura/FaturaService.java:75-109` — `pagarFatura()` linha 101
- `src/main/java/dev/teamwin/contafacil/comprasCartao/ComprasService.java:35-62` — `lancarCompra()` linha 56-58

**Problema:** Exploit de cancelamento pós-pagamento permite gerar limite de crédito infinito:

| Passo | Ação | `limiteUtilizado` | `conta.saldo` |
|---|---|---|---|
| 1 | `lancarCompra(R$500)` | +500 = 500 | inalterado |
| 2 | `pagarFatura(R$500)` | -500 = 0 | -500 |
| 3 | `cancelarCompra(id)` | -500 = **-500** | -500 |

Resultado: `limiteUtilizado` negativo — o usuário perdeu R$500 mas ganhou limite extra que não deveria ter.

**Causa raiz:** `cancelarCompra()` (linha 77) subtrai do `limiteUtilizado` sem verificar se a fatura já foi paga. Se foi paga, `pagarFatura()` já havia subtraído do `limiteUtilizado` (linha 101), causando subtração dupla. Além disso, `lancarCompra()` (linhas 56-58) reabre fatura com status `PAGA`, permitindo comprar após quitar tudo.

**Sugestão de correção:**
1. Em `cancelarCompra()`: bloquear cancelamento se `fatura.getValorPago().compareTo(BigDecimal.ZERO) > 0`
2. Se permitir cancelamento com fatura paga, devolver o valor pago ao `conta.saldo` proporcionalmente
3. Em `lancarCompra()`: não permitir lançar compra se `fatura.getStatus() == StatusFatura.PAGA`
4. Adicionar `@Transactional` no `cancelarCompra()` e `lancarCompra()` para consistência

---

### 3. Concorrência e Race Condition

**Status:** 🔴 Problema encontrado — **Alta**

**Onde:**
- `src/main/java/dev/teamwin/contafacil/transacao/TransacaoService.java:24` — `depositar()`
- `src/main/java/dev/teamwin/contafacil/transacao/TransacaoService.java:46` — `TED`
- `src/main/java/dev/teamwin/contafacil/transacao/TransacaoService.java:90` — `Saque`
- `src/main/java/dev/teamwin/contafacil/fatura/FaturaService.java:75` — `pagarFatura()` sem `@Transactional`
- `src/main/java/dev/teamwin/contafacil/comprasCartao/ComprasService.java:35` — `lancarCompra()` sem `@Transactional`
- `src/main/java/dev/teamwin/contafacil/comprasCartao/ComprasService.java:64` — `cancelarCompra()` sem `@Transactional`

**Problema:** Dois tipos de race condition:

**Tipo A — Lost Update (depósitos concorrentes):** Mesmo com `@Transactional` (isolation READ_COMMITTED padrão), duas requisições simultâneas de depósito causam perda de dinheiro. O JPA faz `UPDATE conta SET saldo = ?` (valor absoluto), não `UPDATE conta SET saldo = saldo + ?` (incremento atômico). Duas threads que leram saldo=100 e depositam 50 e 100 respectivamente resultam em saldo=200 (perdeu 50).

**Tipo B — Inconsistência por falta de `@Transactional`:** `lancarCompra()` e `cancelarCompra()` executam múltiplos `repository.save()` em transações separadas. Se o servidor cair entre dois saves, o banco fica inconsistente (ex: compra salva mas limite não consumido).

**Sugestão de correção:**
1. Adicionar `@Version private Long version;` no `ContaDomain` para optimistic locking
2. Ou usar `@Lock(LockModeType.PESSIMISTIC_WRITE)` na query `findByUserId` nos métodos de transação
3. Adicionar `@Transactional` em todos os métodos listados
4. Considerar usar `UPDATE conta SET saldo = saldo + :valor` via `@Modifying @Query` para operações atômicas no banco

---

### 4. Exposição de dados sensíveis na resposta

**Status:** ⚠️ Atenção

**Onde:**
- `src/main/java/dev/teamwin/contafacil/cartao/CartaoDadosSensiveisDTO.java` — expõe `numeroCartao` (16 dígitos) e `cvv`
- `src/main/java/dev/teamwin/contafacil/cartao/CartaoMapper.java:28-30` — `toDadosSensiveis()` mapeia dados completos
- `src/main/java/dev/teamwin/contafacil/cartao/CartaoResponseDTO.java` — expõe `id` (PK interna)
- `src/main/java/dev/teamwin/contafacil/comprasCartao/CompraCartaoResponseDTO.java` — expõe `id`, `faturaId`
- `src/main/java/dev/teamwin/contafacil/transacao/ExtratoResponseDTO.java` — expõe `contaId`

**Problema:** O `CartaoDadosSensiveisDTO` contém número completo do cartão e CVV — dados suficientes para compras online. Embora atualmente nenhum endpoint o exponha, o método `toDadosSensiveis()` existe pronto para uso. Os IDs internos (PKs) expostos permitem enumeração de entidades e vazam o tamanho da base (IDs auto-incremento). O CVV está armazenado no banco — violação PCI-DSS (CVV deve ser validado e descartado).

**Sugestão de correção:**
1. Remover `CartaoDadosSensiveisDTO` e `toDadosSensiveis()` se não usados
2. Remover CVV do banco — validar na emissão e descartar
3. Substituir IDs internos por UUIDs públicos nas respostas ou usar `@JsonIgnore` nos IDs
4. Retornar 404 em vez de 403 quando entidade pertence a outro usuário (não vazar existência)

---

### 5. Validação de tipos e limites //resolvido

**Status:** 🔴 Problema encontrado — **Média**

**Onde:**
- `src/main/java/dev/teamwin/contafacil/transacao/DepositoRequestDTO.java:9` — sem `@DecimalMax`
- `src/main/java/dev/teamwin/contafacil/transacao/SaqueRequestDTO.java:9` — sem `@DecimalMax`
- `src/main/java/dev/teamwin/contafacil/transacao/TedRequestDTO.java:8` — sem `@DecimalMax`
- `src/main/java/dev/teamwin/contafacil/comprasCartao/CompraCartaoRequestDTO.java:7` — sem `@DecimalMax`
- `src/main/java/dev/teamwin/contafacil/fatura/PagamentoFaturaRequestDTO.java:7` — sem `@DecimalMax`

**Problema:** É possível enviar valores como R$ 999.999.999.999,99 que passam pela validação do Spring. Embora o banco limite via `precision = 19, scale = 2`, a validação deveria ocorrer na aplicação. Valores com mais de 2 casas decimais podem causar arredondamento inesperado no mapeamento JPA/BigDecimal.

**Sugestão de correção:**
- Adicionar `@DecimalMax(value = "1000000.00")` em todos os DTOs com valor monetário //resolvido
- Adicionar `@Digits(integer = 7, fraction = 2)` para garantir precisão correta // resolvido
- Criar annotation customizada `@ValorMonetarioValido` combinando as validações //

---

### 6. Consistência transacional

**Status:** 🔴 Problema encontrado — **Crítica**

**Onde:** Levantamento completo de `@Transactional` por método:

| Método | Arquivo | `@Transactional`? |
|---|---|---|
| `depositar()` | `TransacaoService.java:24` | ✅ Sim |
| `Saque()` | `TransacaoService.java:90` | ✅ Sim |
| `Ted()` | `TransacaoService.java:46` | ✅ Sim |
| `pagarFatura()` | `FaturaService.java:75` | 🔴 Não | //resolvido
| `obterOuCriarFaturaCompetencia()` | `FaturaService.java:30` | 🔴 Não | //resolvido
| `lancarCompra()` | `ComprasService.java:35` | 🔴 Não |  //resolvido
| `cancelarCompra()` | `ComprasService.java:64` | 🔴 Não | //resolvido
| `emitirCartao()` | `CartaoService.java:28` | 🔴 Não | //resolvido
| `ativarCartao()` | `CartaoService.java:56` | 🔴 Não | //resolvido
| `inativarCartao()` | `CartaoService.java:67` | 🔴 Não | //resolvido
| `solicitarLimite()` | `CartaoService.java:78` | 🔴 Não | //resolvido
| `cancelarCartao()` | `CartaoService.java:103` | 🔴 Não | //resolvido
| `abrirConta()` | `ContaService.java:27` | 🔴 Não | //resolvido
| `encerrarConta()` | `ContaService.java:81` | 🔴 Não | //resolvido

**Problema:** Apenas 3 dos 14 métodos que modificam dados têm `@Transactional`. Os 11 métodos sem a anotação executam cada `repository.save()` em uma transação separada (auto-commit JPA). Se o servidor cair entre dois saves consecutivos, o banco fica permanentemente inconsistente.

Exemplo — `cancelarCompra()`:
```
Linha 77: cartao.setLimiteUtilizado(subtract)  → AUTO-COMMIT
Linha 78: cartaoRepository.save(cartao)          → AUTO-COMMIT
Linha 81: fatura.setValorTotal(subtract)         → AUTO-COMMIT (crash aqui: limite já devolvido, fatura não)
Linha 82: faturaRepository.save(fatura)          → AUTO-COMMIT
Linha 84: compra.setStatus(CANCELADA)            → AUTO-COMMIT (crash aqui: tudo alterado menos status)
```

Outro caso crítico — `encerrarConta()`: faz validações de saldo, cartão ativo e fatura aberta em transações separadas. Entre a verificação e o delete, outra thread pode depositar dinheiro, emitir cartão ou gerar fatura.

**Sugestão de correção:**
1. Adicionar `@Transactional` a todos os 11 métodos listados como "Não"
2. Para `encerrarConta()`, usar `@Transactional` com verificação atômica ou query condicional
3. Para os 3 que já têm `@Transactional`, complementar com `@Lock(PESSIMISTIC_WRITE)` na consulta `findByUserId` ou adicionar `@Version` no `ContaDomain`
4. Revisar todos os métodos para garantir rollback completo em caso de exceção

---

## Tabela-Resumo Geral

| ID | Item | Status | Severidade |
|---|---|---|---|
| A01 | Broken Access Control | ⚠️ Atenção | Média |
| A02 | Cryptographic Failures | 🔴 Vulnerabilidade | **Crítica** |
| A03 | Injection | ✅ OK | — |
| A04 | Insecure Design | ⚠️ Atenção | Média |
| A05 | Security Misconfiguration | 🔴 Vulnerabilidade | **Alta** |
| A06 | Vulnerable Components | ⚠️ Atenção | Média |
| A07 | Authentication Failures | 🔴 Vulnerabilidade | **Alta** |
| A08 | Data Integrity | ✅ OK | — |
| A09 | Logging Failures | 🔴 Vulnerabilidade | **Alta** |
| A10 | SSRF | ✅ OK | — |
| B1 | Rate Limiting | 🔴 Problema | **Alta** |
| B2 | Regras de negócio exploráveis | 🔴 Problema | **Crítica** |
| B3 | Race Condition | 🔴 Problema | **Alta** |
| B4 | Dados sensíveis expostos | ⚠️ Atenção | Média |
| B5 | Validação de limites | 🔴 Problema | Média |
| B6 | Consistência transacional | 🔴 Problema | **Crítica** |

### Prioridades de correção

1. **Imediata:** Chave JWT (A02), exploit de cancelamento pós-pagamento (B2), `@Transactional` ausente (B6)
2. **Alta:** Rate limiting (B1), race condition (B3), senhas fracas (A07), logging (A09), CORS + credenciais (A05)
3. **Média:** Spring Boot 4.0.x (A06), controle de acesso (A01), dados sensíveis (B4), validação de limites (B5)
4. **Baixa:** Mensagens de erro inconsistentes (A04), IDs internos expostos (B4)
