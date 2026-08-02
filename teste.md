# Testes de Integração — ContaFácil

## Por que Testcontainers + Docker

Testes de integração com banco de verdade têm um problema clássico: onde rodar os dados?

**Opção A: MySQL da máquina (compartilhado)**

```
Dev mexendo no banco  →  seed de teste polui as tabelas  →  resultado imprevisível
Teste A cria registro  →  Teste B vê registro  →  ordem importa (frágil)
Rodou o teste  →  lixo no banco  →  precisa limpar manual
Outro dev clona  →  precisa instalar MySQL  →  atrito
```

**Opção B: Testcontainers + Docker**

```
mvn test
   ↓
Testcontainers sobe container MySQL vazio
   ↓
Flyway roda as migrations (mesmo V1..V7)
   ↓
Testes executam
   ↓
Mata container
   ↓
Banco da máquina intacto
```

Cada execução começa do zero. Determinístico. Zero sujeira. Sem dependência de MySQL instalado — só Docker.

---

## Dependências no `pom.xml`

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

A versão é resolvida pelo `spring-boot-starter-parent`. Não precisa colocar `<version>`.

---

## Estrutura atual

```
src/test/java/dev/teamwin/contafacil/
├── ContaFacilApplicationTests.java
├── BaseIntegrationTest.java            ← @SpringBootTest + @Transactional + importa config
├── config/
│   └── TestcontainerConfig.java        ← sobe o container MySQL
└── transacao/
    └── TransacaoServiceTest.java       ← extends BaseIntegrationTest
```

---

## TestcontainerConfig.java

```java
package dev.teamwin.contafacil.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainerConfig {

    public static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("contafacil_test")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL.start();
    }

    @Bean
    public DynamicPropertyRegistrar dynamicPropertiesRegistrar() {
        return registry -> {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        };
    }
}
```

O `DynamicPropertyRegistrar` (Spring Boot 4.x) sobrescreve as propriedades de datasource antes do Spring criar a conexão — assim os testes apontam pro container, não pro MySQL da máquina.

---

## BaseIntegrationTest.java

```java
package dev.teamwin.contafacil;

import dev.teamwin.contafacil.config.TestcontainerConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ContextConfiguration(classes = {TestcontainerConfig.class})
public class BaseIntegrationTest {

}
```

| Anotação | O que faz |
|---|---|
| `@SpringBootTest` | Sobe o contexto Spring completo |
| `@Transactional` | Cada teste roda dentro de uma transação. No final, ROLLBACK. Dados nunca persistem |
| `@ContextConfiguration` | Importa a config que sobe o container MySQL |

---

## JUnit vs AssertJ

JUnit fornece a **estrutura**: `@Test`, `@BeforeEach`, `@AfterEach`.  
AssertJ fornece as **asserções legíveis**: `assertThat(...).isEqualByComparingTo(...)`.

```java
import org.junit.jupiter.api.Test;            // JUnit
import org.junit.jupiter.api.BeforeEach;       // JUnit
import static org.assertj.core.api.Assertions.*; // AssertJ
```

| JUnit puro | AssertJ | Por quê |
|---|---|---|
| `assertEquals(0, saldo.compareTo(expected))` | `assertThat(saldo).isEqualByComparingTo("500")` | BigDecimal com `compareTo` |
| `assertThrows(X.class, () -> codigo)` | `assertThatThrownBy(() -> codigo)` | Mais legível |
| `assertTrue(lista.isEmpty())` | `assertThat(lista).isEmpty()` | Mensagem de erro melhor |

---

## Autenticação nos testes

O `TransacaoService` faz:

```java
UserDomain user = (UserDomain) SecurityContextHolder.getContext()
        .getAuthentication()
        .getPrincipal();
```

`@WithMockUser` não funciona porque coloca um `UserDetails` genérico, não um `UserDomain`. Solução: criar o `UserDomain` real no banco e injetar manualmente no `SecurityContextHolder`.

```java
private UserDomain criarEautenticarUsuario(String email) {
    UserDomain user = new UserDomain();
    user.setEmail(email);
    user.setUsername(email.split("@")[0]);
    user.setPasswordHash("hash");
    user = userRepository.save(user);

    var authorities = Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_USER")
    );
    var authentication = new UsernamePasswordAuthenticationToken(
        user, null, authorities
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    return user;
}
```

---

## TransacaoServiceTest — completo

```java
package dev.teamwin.contafacil.transacao;

import dev.teamwin.contafacil.BaseIntegrationTest;
import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.user.UserDomain;
import dev.teamwin.contafacil.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

class TransacaoServiceTest extends BaseIntegrationTest {

    @Autowired
    private TransacaoService transacaoService;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private UserRepository userRepository;

    private UserDomain usuario;
    private ContaDomain conta;

    @BeforeEach
    void setUp() {
        usuario = new UserDomain();
        usuario.setEmail("joao@email.com");
        usuario.setUsername("joao");
        usuario.setPasswordHash("hash");
        usuario = userRepository.save(usuario);

        autenticar(usuario);

        conta = new ContaDomain();
        conta.setContaCorrente("000001");
        conta.setAgencia("0001");
        conta.setSaldo(new BigDecimal("1000"));
        conta.setUser(usuario);
        conta = contaRepository.save(conta);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(UserDomain user) {
        var authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        var authentication = new UsernamePasswordAuthenticationToken(
                user, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ──── DEPÓSITO ────

    @Test
    void deposito_deveAumentarSaldo() {
        var dto = new DepositoRequestDTO(new BigDecimal("500"), null);

        transacaoService.depositar(dto);

        ContaDomain atualizada = contaRepository
                .findByUserId(usuario.getId()).get(0);
        assertThat(atualizada.getSaldo()).isEqualByComparingTo("1500");
    }

    // ──── SAQUE ────

    @Test
    void saque_deveDiminuirSaldo() {
        var dto = new SaqueRequestDTO(new BigDecimal("300"), null);

        transacaoService.Saque(dto);

        ContaDomain atualizada = contaRepository
                .findByUserId(usuario.getId()).get(0);
        assertThat(atualizada.getSaldo()).isEqualByComparingTo("700");
    }

    @Test
    void saque_comSaldoInsuficiente_lancaExcecao() {
        var dto = new SaqueRequestDTO(new BigDecimal("999999"), null);

        assertThatThrownBy(() -> transacaoService.Saque(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Saldo insuficiente");

        ContaDomain atualizada = contaRepository
                .findByUserId(usuario.getId()).get(0);
        assertThat(atualizada.getSaldo()).isEqualByComparingTo("1000");
    }

    // ──── TED ────

    @Test
    void ted_deveTransferirSaldo() {
        UserDomain destinoUser = new UserDomain();
        destinoUser.setEmail("maria@email.com");
        destinoUser.setUsername("maria");
        destinoUser.setPasswordHash("hash");
        destinoUser = userRepository.save(destinoUser);

        ContaDomain destino = new ContaDomain();
        destino.setContaCorrente("000002");
        destino.setAgencia("0001");
        destino.setSaldo(new BigDecimal("500"));
        destino.setUser(destinoUser);
        destino = contaRepository.save(destino);

        var dto = new TedRequestDTO(
                new BigDecimal("300"), null, destino.getContaCorrente()
        );

        transacaoService.ted(dto);

        ContaDomain origemAtual = contaRepository
                .findByUserId(usuario.getId()).get(0);
        ContaDomain destinoAtual = contaRepository
                .findByUserId(destinoUser.getId()).get(0);

        assertThat(origemAtual.getSaldo()).isEqualByComparingTo("700");
        assertThat(destinoAtual.getSaldo()).isEqualByComparingTo("800");
    }

    @Test
    void ted_comSaldoInsuficiente_lancaExcecao() {
        UserDomain destinoUser = new UserDomain();
        destinoUser.setEmail("maria@email.com");
        destinoUser.setUsername("maria");
        destinoUser.setPasswordHash("hash");
        destinoUser = userRepository.save(destinoUser);

        ContaDomain destino = new ContaDomain();
        destino.setContaCorrente("000002");
        destino.setAgencia("0001");
        destino.setSaldo(BigDecimal.ZERO);
        destino.setUser(destinoUser);
        destino = contaRepository.save(destino);

        var dto = new TedRequestDTO(
                new BigDecimal("999999"), null, destino.getContaCorrente()
        );

        assertThatThrownBy(() -> transacaoService.ted(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Saldo insuficiente");

        ContaDomain origemAtual = contaRepository
                .findByUserId(usuario.getId()).get(0);
        assertThat(origemAtual.getSaldo()).isEqualByComparingTo("1000");
    }

    @Test
    void ted_paraPropriaConta_lancaExcecao() {
        var dto = new TedRequestDTO(
                new BigDecimal("300"), null, conta.getContaCorrente()
        );

        assertThatThrownBy(() -> transacaoService.ted(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Nao e possivel transferir para a propria conta");
    }
}
```

---

## Cobertura de testes

### TransacaoServiceTest — 6 testes

| # | Teste | O que verifica |
|---|---|---|
| 1 | `deposito_deveAumentarSaldo` | Saldo 1000 + 500 = 1500 |
| 2 | `saque_deveDiminuirSaldo` | Saldo 1000 - 300 = 700 |
| 3 | `saque_comSaldoInsuficiente_lancaExcecao` | Exceção, saldo inalterado 1000 |
| 4 | `ted_deveTransferirSaldo` | Origem 700, Destino 800 |
| 5 | `ted_comSaldoInsuficiente_lancaExcecao` | Exceção, origem inalterada 1000 |
| 6 | `ted_paraPropriaConta_lancaExcecao` | Exceção |

### FaturaServiceTest — 5 testes (pendente)

| # | Teste |
|---|---|
| 1 | `pagamentoTotal_deveZerarPendente` |
| 2 | `pagamentoParcial_deveManterAberta` |
| 3 | `pagamentoMaiorQuePendente_lancaExcecao` |
| 4 | `pagamentoFaturaJaPaga_lancaExcecao` |
| 5 | `pagamento_deveCriarTransacaoAuditoria` |

### ComprasServiceTest — 6 testes (pendente)

| # | Teste |
|---|---|
| 1 | `lancarCompra_deveConsumirLimite` |
| 2 | `cancelarCompra_faturaAberta_deveLiberarLimite` |
| 3 | `cancelarCompra_faturaPaga_lancaExcecao` |
| 4 | `estornarCompra_faturaPaga_deveRestituirSaldo` |
| 5 | `estornarCompra_faturaAberta_lancaExcecao` |
| 6 | `cancelarCompra_jaCancelada_lancaExcecao` |

---

## Como executar

```bash
mvn test                                         # todos
mvn test -Dtest=TransacaoServiceTest             # só um
mvn test -Dtest=TransacaoServiceTest#deposito_deveAumentarSaldo  # um método
```

---

## Regras de ouro

### 1. `BigDecimal` sempre com `isEqualByComparingTo`

```java
assertThat(saldo).isEqualByComparingTo("1500");  // certo
assertThat(saldo).isEqualTo(new BigDecimal("1500")); // errado (escala)
```

### 2. `@Transactional` faz rollback no final de cada `@Test`

Dados criados no `@BeforeEach` somem ao final do teste. Container volta ao estado pós-migration. Próximo teste começa zerado.

### 3. `@AfterEach` limpa o `SecurityContextHolder`

O `SecurityContextHolder` é estático. Se você não limpar, a autenticação de um teste vaza pro próximo.

### 4. Exceções com `assertThatThrownBy`

```java
assertThatThrownBy(() -> service.metodo(dto))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Saldo insuficiente");
```
