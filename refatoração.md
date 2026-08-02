# Refatoração — Backend ContaFácil

Auditoria completa — julho/2026. `encerrarConta()` excluído do escopo (não foi a produção).

---

## CRÍTICO (3)

### C1. `cancelarCompra()` permite cancelamento em fatura PAGA — corrompe dados  // Corrigido

**Arquivo:** `comprasCartao/ComprasService.java:71-94`

```java
public CompraCartaoResponseDTO cancelarCompra(Long compraId){
    ...
    cartao.setLimiteUtilizado(cartao.getLimiteUtilizado().subtract(compra.getValor())); // linha 84
}
```

Nenhuma validação do status da fatura. Em fatura `PAGA`, o pagamento já zerou `limiteUtilizado`. Subtrair de novo deixa o valor **negativo**, violando `ck_cartoes_limite_utilizado_non_negative CHECK (limite_utilizado >= 0)`. O banco rejeita e o cliente recebe erro 500.

**Cenário real:** Cliente paga fatura, clica "Cancelar Compra", erro interno do servidor.

**Correção:**
```java
if (compra.getFatura().getStatus() != StatusFatura.ABERTA) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Cancelamento disponível apenas para faturas em aberto. Use estornar para faturas pagas.");
}
```

---

### C2. `emitirCartao()` bloqueia nova emissão com cartão INATIVO/BLOQUEADO  // Sim, será bloqueado, no momento é 1 cartão só

**Arquivo:** `cartao/CartaoService.java:37-43`

```java
boolean temCartaoAtivo = cartaoRepository.findByContaId(conta.getId())
    .stream()
    .anyMatch(c -> c.getStatus() != StatusCartao.CANCELADO);
```

O nome da variável diz "ativo" mas a condição é "qualquer coisa menos CANCELADO". Um cartão `INATIVO` (emitido, não ativado) ou `BLOQUEADO` bloqueia a emissão de um novo.

**Cenário real:** Cliente emite cartão, perde o código de ativação, tenta emitir outro. Erro: "Conta já possui um cartão". Fica preso.

**Correção:**
```java
boolean temCartaoAtivo = cartaoRepository.findByContaId(conta.getId())
    .stream()
    .anyMatch(c -> c.getStatus() == StatusCartao.ATIVO);
```

---

### C3. `solicitarLimite()` não valida se o cartão está ATIVO   // não há nenhum problema nisso

**Arquivo:** `cartao/CartaoService.java:90-101`

```java
public CartaoResponseDTO solicitarLimite(Long cartaoId){
    CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);
    if (cartao.getLimiteTotal().compareTo(BigDecimal.ZERO) > 0) {
        throw new ResponseStatusException(...);
    }
    BigDecimal limite = BigDecimal.valueOf(1000 + new Random().nextInt(1501));
    ...
}
```

Compare com `lancarCompra()` que chama `validarCartaoAtivoEValido(cartao)`. Um cartão `INATIVO` pode receber limite sem nunca ter sido ativado.

**Cenário real:** Cliente emite cartão, não ativa, solicita limite. Recebe limite mas nunca poderá usar o cartão.

**Correção:** Adicionar `validarCartaoAtivoEValido(cartao)` no início do método.

---

## ALTO (7)

### A1. `Ted()` — nome do método quebra convenção Java       // resolvido, mas não era alto

**Arquivo:** `transacao/TransacaoService.java:54`

```java
public TransacaoResponseDTO Ted(TedRequestDTO dto)
```

Métodos Java começam com minúscula. O endpoint funciona mas fere o style guide e confunde ferramentas de análise.

---

### A2. Loggers apontam para `AuthService.class` em vez da própria classe 

**Arquivos:** `transacao/TransacaoService.java:27`, `comprasCartao/ComprasService.java:38`

```java
private static final Logger log = LoggerFactory.getLogger(AuthService.class);
```

Logs de `TransacaoService` e `ComprasService` aparecem como se fossem de `AuthService` nos arquivos de log. Debugging impossível.

**Correção:** Trocar para `LoggerFactory.getLogger(TransacaoService.class)` e `LoggerFactory.getLogger(ComprasService.class)`.

---

### A3. `obterOuCriarFaturaCompetencia()` — `dataFechamento` e `dataVencimento` são a mesma data

**Arquivo:** `fatura/FaturaService.java:46-47`

```java
fatura.setDataFechamento(LocalDate.of(ano, mes, 6).plusMonths(1));
fatura.setDataVencimento(LocalDate.of(ano, mes, 6).plusMonths(1));
```

Fechamento e vencimento idênticos. Em cartão real, o vencimento é ~10 dias após o fechamento. Para simulação, não quebra nada, mas é irrealista e um campo `dataFechamento` igual ao `dataVencimento` é inútil.

---

### A4. `TokenService` — timezone hardcoded `-03:00` (Brasília)

**Arquivo:** `infra/security/TokenService.java:47`

```java
return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
```

Em produção, qualquer usuário fora do fuso de Brasília tem o token com expiração errada. O correto é usar `ZoneOffset.UTC` ou `ZoneId.systemDefault()`.

**Correção:**
```java
return LocalDateTime.now(ZoneOffset.UTC).plusHours(2).toInstant(ZoneOffset.UTC);
```

---

### A5. `StatusCartao.BLOQUEADO` existe no enum mas nunca é usado

**Arquivo:** `cartao/StatusCartao.java`

O enum define `ATIVO, INATIVO, BLOQUEADO, CANCELADO`. O controller tem endpoint `bloquearCartao` mas chama `cartaoService.inativarCartao()` que seta `INATIVO`, não `BLOQUEADO`. O status `BLOQUEADO` é inalcançável — código morto no enum.

---

### A6. `cancelarCartao()` não valida faturas do cartão cancelado

**Arquivo:** `cartao/CartaoService.java:116-127`

```java
public String cancelarCartao(Long cartaoId) {
    ...
    boolean temFaturaPendente = faturaRepository
            .findByCartaoIdOrderByAnoDescMesDesc(cartaoId)
            .stream()
            .anyMatch(f -> f.getValorPendente().compareTo(BigDecimal.ZERO) > 0);
```

A validação existe (corrigida da auditoria anterior), mas o cancelamento ainda força o usuário a bloquear o cartão primeiro (`INATIVO` → `CANCELADO`). Um cartão `ATIVO` com fatura pendente não pode ser cancelado diretamente — a mensagem diz "Bloqueie o cartão antes de cancelar". Isso é intencional ou um bug de UX?

---

### A7. `verDadosSensiveis()` expõe CVV de cartões CANCELADO

**Arquivo:** `cartao/CartaoService.java:129-134`

```java
public CartaoDadosSensiveisDTO verDadosSensiveis(Long cartaoId) {
    CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);
    return cartaoMapper.toDadosSensiveis(cartao);
}
```

Nenhuma validação de status. Um cartão `CANCELADO` que não pertence mais ao usuário (cancelado por inatividade, por exemplo) ainda expõe número completo e CVV. Dado sensível sem controle de acesso por status.

---

## MÉDIO (5)

### M1. `ContaMapper.toResponse()` calcula variáveis que não são usadas no DTO

**Arquivo:** `conta/ContaMapper.java:11-14`

```java
public ContaResponseDTO toResponse(ContaDomain conta) {
    String nome = conta.getUser() != null ? conta.getUser().getUsername() : null;     // não usado
    Long idUsuario = conta.getUser() != null ? conta.getUser().getId() : null;        // não usado
    return new ContaResponseDTO(conta.getContaCorrente(), conta.getAgencia(), conta.getSaldo());
}
```

Código morto. Se o DTO não precisa desses campos, o cálculo é desperdício.

---

### M2. `V5__add_status_compra.sql` — default `PENDENTE` não existe no enum

**Arquivo:** `V5__add_status_compra.sql`

```sql
ALTER TABLE compras_cartao ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';
```

O enum `StatusCompra` tem `AUTORIZADA, NEGADA, CANCELADA`. `PENDENTE` não existe. O `lancarCompra` sempre define `AUTORIZADA` antes de salvar, então o default nunca é usado. Mas se alguma migration futura ou script manual inserir uma compra sem status, o JPA não consegue mapear e lança exceção.

---

### M3. Duplicação de métodos privados entre services

Os métodos abaixo são **idênticos** em 4 services (`CartaoService`, `ComprasService`, `FaturaService`, `ContaService`):

```java
private UserDomain getUsuarioAutenticado() { ... }
private ContaDomain getContaUsuario(UserDomain user) { ... }
private CartaoDomain getCartaoUsuario(Long cartaoId, ContaDomain conta) { ... }  // 3 services
```

Qualquer mudança na lógica de busca de conta ou autenticação exige alterar 4 arquivos. Risco de inconsistência entre eles.

---

### M4. `validarCartaoAtivoEValido` e `validarLimiteDisponivel` duplicados em `CartaoService`

**Arquivo:** `cartao/CartaoService.java:172-185`

Esses métodos existem em `CartaoService` e `ComprasService` com código idêntico. Só são chamados de `ComprasService.lancarCompra()`. Dentro de `CartaoService` nunca são usados — código morto.

---

### M5. `ComprasCartaoDTO` declarado mas nunca usado

**Arquivo:** `comprasCartao/ComprasCartaoDTO.java`

O DTO existe, tem validações, o mapper tem método `toDto()` que o mapeia, mas nenhum controller ou service retorna ou recebe `ComprasCartaoDTO`. Código morto com anotações de validação que nunca são executadas.

---

## BAIXO (3)

### B1. Sem mecanismo de revogação de token JWT

Tokens JWT são stateless — não dá pra invalidar individualmente. Se um token vaza, ele é válido por 2 horas e não há como revogar. Refresh token com blacklist resolveria.

### B2. `ResponseDTO` — campo `name` contém `username`

```java
public record ResponseDTO(String name, String token) {}
// preenchido com domain.getUsername()
```

O campo chama `name`, o valor é `username`. Confunde o frontend.

### B3. `CartaoService.emitirCartao` — mensagem genérica "Conta já possui um cartão"

Quando o cartão não está CANCELADO mas também não está ATIVO (ex: INATIVO), a mensagem não ajuda o usuário a entender que ele pode cancelar o INATIVO e emitir outro. A mensagem deveria diferenciar os casos.

---

## RESUMO

| Severidade | Quantidade | Principais áreas |
|---|---|---|
| **CRÍTICO** | 3 | `cancelarCompra` PAGA (C1), `emitirCartao` INATIVO (C2), `solicitarLimite` INATIVO (C3) |
| **ALTO** | 7 | Nomenclatura Java (A1), loggers errados (A2), datas iguais (A3), timezone hardcoded (A4), status BLOQUEADO inalcançável (A5), cancelamento dois passos (A6), dados sensíveis expostos (A7) |
| **MÉDIO** | 5 | Código morto no mapper (M1), migration com default inválido (M2), duplicação (M3, M4), DTO não usado (M5) |
| **BAIXO** | 3 | Sem revogação JWT (B1), name vs username (B2), mensagem genérica (B3) |

Total: 18 pontos de refatoração.
