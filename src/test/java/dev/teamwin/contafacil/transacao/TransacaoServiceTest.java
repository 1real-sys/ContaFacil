package dev.teamwin.contafacil.transacao;

import dev.teamwin.contafacil.BaseIntegrationTest;
import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.infra.security.AuthenticatedUser;
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
                new AuthenticatedUser(user.getId(), user.getEmail(), user.getUsername()),
                null,
                authorities
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
                .hasMessageContaining("Não é possível transferir para a própria conta");
    }
}