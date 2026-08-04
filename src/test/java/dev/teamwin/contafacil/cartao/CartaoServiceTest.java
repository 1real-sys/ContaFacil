package dev.teamwin.contafacil.cartao;

import dev.teamwin.contafacil.BaseIntegrationTest;
import dev.teamwin.contafacil.comprasCartao.ComprasCartaoRepository;
import dev.teamwin.contafacil.comprasCartao.ComprasService;
import dev.teamwin.contafacil.infra.security.AuthenticatedUser;
import dev.teamwin.contafacil.comprasCartao.CompraCartaoRequestDTO;
import dev.teamwin.contafacil.comprasCartao.CategoriaEstabelecimento;
import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.fatura.FaturaService;
import dev.teamwin.contafacil.fatura.PagamentoFaturaRequestDTO;
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

class CartaoServiceTest extends BaseIntegrationTest {

    @Autowired
    private CartaoService cartaoService;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private ComprasService comprasService;

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private ComprasCartaoRepository comprasCartaoRepository;

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

    // ──── Emitir Cartão ────
    @Test
    void emitirCartaoComSucesso(){
        var dto = new CartaoCreateDTO(BandeiraCartao.VISA);

        var response = cartaoService.emitirCartao(dto);

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(StatusCartao.INATIVO);
        assertThat(response.bandeira()).isEqualTo(BandeiraCartao.VISA);
        assertThat(response.limiteTotal()).isNotNull();

    }

    // ──── Ativar Cartão ────
    @Test
    void ativarCartaoComSucesso(){
        var dto = new CartaoCreateDTO(BandeiraCartao.MASTERCARD);
        var cartaoResponse = cartaoService.emitirCartao(dto);

        var response = cartaoService.ativarCartao(cartaoResponse.id());

        assertThat(response.status()).isEqualTo(StatusCartao.ATIVO);
        assertThat(response.id()).isEqualTo(cartaoResponse.id());

    }

    // ──── Inativar Cartão ────
    @Test
    void inativarCartaoComSucesso(){
        var dto = new CartaoCreateDTO(BandeiraCartao.VISA);
        var cartaoResponse = cartaoService.emitirCartao(dto);

        cartaoService.ativarCartao(cartaoResponse.id());
        var response = cartaoService.inativarCartao(cartaoResponse.id());

        assertThat(response.status()).isEqualTo(StatusCartao.INATIVO);
        assertThat(response.id()).isEqualTo(cartaoResponse.id());
    }

    // ──── Cancelar Cartão ────
    @Test
    void cancelarCartaoComSucesso(){
        var dto = new CartaoCreateDTO(BandeiraCartao.VISA);
        var cartaoResponse = cartaoService.emitirCartao(dto);

        cartaoService.ativarCartao(cartaoResponse.id());
        cartaoService.inativarCartao(cartaoResponse.id());

        var response = cartaoService.cancelarCartao(cartaoResponse.id());

        assertThat(response).isEqualTo("Cartão cancelado com sucesso");

        var cartaoSalvo = cartaoRepository.findById(cartaoResponse.id()).get();
        assertThat(cartaoSalvo.getStatus()).isEqualTo(StatusCartao.CANCELADO);
    }

    @Test
    void solicitarLimiteComSucesso(){
        var dto = new CartaoCreateDTO(BandeiraCartao.VISA);
        var cartaoResponse = cartaoService.emitirCartao(dto);

        cartaoService.ativarCartao(cartaoResponse.id());

        var response = cartaoService.solicitarLimite(cartaoResponse.id());

        assertThat(response.limiteTotal()).isGreaterThanOrEqualTo(new BigDecimal("1000"));
    }

    // ──── C1: cancelarCompra em fatura PAGA deve lançar exceção ────
    @Test
    void cancelarCompra_emFaturaPaga_deveLancarExcecao() {
        var dto = new CartaoCreateDTO(BandeiraCartao.VISA);
        var cartaoResponse = cartaoService.emitirCartao(dto);
        cartaoService.ativarCartao(cartaoResponse.id());
        cartaoService.solicitarLimite(cartaoResponse.id());

        var compraDto = new CompraCartaoRequestDTO(
                new BigDecimal("500"), "Teste", CategoriaEstabelecimento.SHOPPING
        );
        var compraResponse = comprasService.lancarCompra(cartaoResponse.id(), compraDto);

        var pagamentoDto = new PagamentoFaturaRequestDTO(new BigDecimal("500"));
        faturaService.pagarFatura(compraResponse.faturaId(), pagamentoDto);

        assertThatThrownBy(() -> comprasService.cancelarCompra(compraResponse.id()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cancelamento disponível apenas para faturas em aberto");
    }
}
