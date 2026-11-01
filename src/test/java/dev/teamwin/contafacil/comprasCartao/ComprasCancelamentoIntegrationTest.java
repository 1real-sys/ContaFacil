package dev.teamwin.contafacil.comprasCartao;

import dev.teamwin.contafacil.BaseIntegrationTest;
import dev.teamwin.contafacil.cartao.BandeiraCartao;
import dev.teamwin.contafacil.cartao.CartaoDomain;
import dev.teamwin.contafacil.cartao.CartaoRepository;
import dev.teamwin.contafacil.cartao.StatusCartao;
import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.fatura.FaturaDomain;
import dev.teamwin.contafacil.fatura.FaturaRepository;
import dev.teamwin.contafacil.fatura.FaturaService;
import dev.teamwin.contafacil.fatura.PagamentoFaturaRequestDTO;
import dev.teamwin.contafacil.fatura.StatusFatura;
import dev.teamwin.contafacil.infra.security.AuthenticatedUser;
import dev.teamwin.contafacil.transacao.DescricaoTransacao;
import dev.teamwin.contafacil.transacao.TransacaoDomain;
import dev.teamwin.contafacil.transacao.TransacaoRepository;
import dev.teamwin.contafacil.user.UserDomain;
import dev.teamwin.contafacil.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComprasCancelamentoIntegrationTest extends BaseIntegrationTest {

    @Autowired private ComprasService comprasService;
    @Autowired private FaturaService faturaService;
    @Autowired private UserRepository userRepository;
    @Autowired private ContaRepository contaRepository;
    @Autowired private CartaoRepository cartaoRepository;
    @Autowired private FaturaRepository faturaRepository;
    @Autowired private ComprasCartaoRepository comprasRepository;
    @Autowired private AjusteCompraRepository ajusteRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private EntityManager entityManager;

    private Long contaId;
    private Long cartaoId;

    @BeforeEach
    void prepararContaECartao() {
        UserDomain usuario = criarUsuario("cancelamento@teste.com", "cancelamento");
        autenticar(usuario);

        ContaDomain conta = new ContaDomain();
        conta.setContaCorrente("000001");
        conta.setAgencia("0001");
        conta.setSaldo(valor("1000"));
        conta.setUser(usuario);
        contaId = contaRepository.save(conta).getId();

        CartaoDomain cartao = new CartaoDomain();
        cartao.setConta(conta);
        cartao.setNumeroCartao("4000000000000001");
        cartao.setBandeira(BandeiraCartao.VISA);
        cartao.setStatus(StatusCartao.ATIVO);
        cartao.setLimiteTotal(valor("1000"));
        cartao.setLimiteUtilizado(BigDecimal.ZERO);
        cartaoId = cartaoRepository.save(cartao).getId();
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void semPagamentoCancelaCompraComumSemCredito() {
        var compra = comprar("150");

        comprasService.cancelarCompra(compra.id());
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "0", "0", StatusFatura.ABERTA);
        conferirContaECartao("1000", "0");
        assertThat(comprasRepository.findById(compra.id()).orElseThrow().getStatus()).isEqualTo(StatusCompra.CANCELADA);
        assertThat(ajusteRepository.count()).isZero();
        assertThat(creditos()).isEmpty();
    }

    @Test
    void ajusteSemPagamentoRecusaSemAlterarDados() {
        var compra = comprar("150");

        assertThatThrownBy(() -> comprasService.cancelarCompraComAjuste(compra.id()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "150", "0", StatusFatura.ABERTA);
        conferirContaECartao("1000", "150");
        assertThat(comprasRepository.findById(compra.id()).orElseThrow().getStatus()).isEqualTo(StatusCompra.AUTORIZADA);
        assertThat(ajusteRepository.count()).isZero();
    }

    @Test
    void pagamentoParcialECompraMenorQuePendenteAbateTudoSemCredito() {
        var compra = comprar("100");
        comprar("100");
        pagar(compra.faturaId(), "50");

        comprasService.cancelarCompraComAjuste(compra.id());
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "100", "50", StatusFatura.ABERTA);
        conferirContaECartao("950", "50");
        conferirAjuste(compra.id(), "100", "0");
        assertThat(creditos()).isEmpty();
    }

    @Test
    void pagamentoParcialECompraIgualAoPendenteQuitaFatura() {
        var compra = comprar("100");
        comprar("100");
        pagar(compra.faturaId(), "100");

        comprasService.cancelarCompraComAjuste(compra.id());
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "100", "100", StatusFatura.PAGA);
        conferirContaECartao("900", "0");
        conferirAjuste(compra.id(), "100", "0");
        assertThat(creditos()).isEmpty();
    }

    @Test
    void pagamentoParcialECompraMaiorQuePendenteDivideAbatimentoECredito() {
        var compra = comprar("100");
        comprar("100");
        pagar(compra.faturaId(), "150");

        comprasService.cancelarCompraComAjuste(compra.id());
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "150", "150", StatusFatura.PAGA);
        conferirContaECartao("900", "0");
        conferirAjuste(compra.id(), "50", "50");
        assertThat(creditos()).singleElement().satisfies(credito -> {
            assertThat(credito.getValor()).isEqualByComparingTo("50");
            assertThat(credito.getSaldoAntes()).isEqualByComparingTo("850");
            assertThat(credito.getSaldoDepois()).isEqualByComparingTo("900");
        });
    }

    @Test
    void compraUnicaMaiorQuePendenteCreditaPartePaga() {
        var compra = comprar("150");
        pagar(compra.faturaId(), "100");

        comprasService.cancelarCompraComAjuste(compra.id());
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "100", "100", StatusFatura.PAGA);
        conferirContaECartao("1000", "0");
        conferirAjuste(compra.id(), "50", "100");
        assertThat(creditos()).singleElement().satisfies(credito ->
                assertThat(credito.getValor()).isEqualByComparingTo("100"));
    }

    @Test
    void faturaPagaCreditaCompraInteiraSemMexerNoValorPago() {
        var compra = comprar("100");
        pagar(compra.faturaId(), "100");

        comprasService.cancelarCompraComAjuste(compra.id());
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "100", "100", StatusFatura.PAGA);
        conferirContaECartao("1000", "0");
        conferirAjuste(compra.id(), "0", "100");
        assertThat(creditos()).hasSize(1);
    }

    @Test
    void cancelamentoRepetidoNaoCreditaDuasVezes() {
        var compra = comprar("100");
        pagar(compra.faturaId(), "100");
        comprasService.cancelarCompraComAjuste(compra.id());

        assertThatThrownBy(() -> comprasService.cancelarCompraComAjuste(compra.id()))
                .isInstanceOf(ResponseStatusException.class);
        recarregarEntidades();

        conferirContaECartao("1000", "0");
        assertThat(ajusteRepository.count()).isEqualTo(1);
        assertThat(creditos()).hasSize(1);
    }

    @Test
    void duasComprasCanceladasEmSequenciaNaoDevolvemMaisQueOFoiPago() {
        var primeiraCompra = comprar("100");
        var segundaCompra = comprar("100");
        pagar(primeiraCompra.faturaId(), "150");

        comprasService.cancelarCompraComAjuste(primeiraCompra.id());
        comprasService.cancelarCompraComAjuste(segundaCompra.id());
        recarregarEntidades();

        conferirFatura(primeiraCompra.faturaId(), "150", "150", StatusFatura.PAGA);
        conferirContaECartao("1000", "0");
        assertThat(ajusteRepository.findAll()).hasSize(2);
        assertThat(creditos()).extracting(TransacaoDomain::getValor)
                .containsExactlyInAnyOrder(valor("50.00"), valor("100.00"));
    }

    @Test
    void outroUsuarioNaoConsegueCancelarCompra() {
        var compra = comprar("100");
        pagar(compra.faturaId(), "50");
        UserDomain outro = criarUsuario("outro@teste.com", "outro");
        ContaDomain outraConta = new ContaDomain();
        outraConta.setContaCorrente("000002");
        outraConta.setAgencia("0001");
        outraConta.setSaldo(valor("1000"));
        outraConta.setUser(outro);
        contaRepository.save(outraConta);
        autenticar(outro);

        assertThatThrownBy(() -> comprasService.cancelarCompraComAjuste(compra.id()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "100", "50", StatusFatura.ABERTA);
        assertThat(ajusteRepository.count()).isZero();
    }

    @Test
    void cancelamentoComumNaoPodeAlterarFaturaComPagamentoParcial() {
        var compra = comprar("100");
        comprar("100");
        pagar(compra.faturaId(), "150");

        assertThatThrownBy(() -> comprasService.cancelarCompra(compra.id()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "200", "150", StatusFatura.ABERTA);
        conferirContaECartao("850", "50");
        assertThat(ajusteRepository.count()).isZero();
    }

    @Test
    void rotaAntigaDeEstornoSegueARegraDoCreditoDeConfianca() {
        var compra = comprar("100");
        pagar(compra.faturaId(), "100");

        comprasService.estornarCompra(compra.id());
        recarregarEntidades();

        conferirFatura(compra.faturaId(), "100", "100", StatusFatura.PAGA);
        conferirContaECartao("1000", "0");
        conferirAjuste(compra.id(), "0", "100");
        assertThat(creditos()).hasSize(1);
    }

    private CompraCartaoResponseDTO comprar(String quantia) {
        return comprasService.lancarCompra(cartaoId,
                new CompraCartaoRequestDTO(valor(quantia), "Teste", CategoriaEstabelecimento.SHOPPING));
    }

    private void pagar(Long faturaId, String quantia) {
        faturaService.pagarFatura(faturaId, new PagamentoFaturaRequestDTO(valor(quantia)));
    }

    private void recarregarEntidades() {
        entityManager.flush();
        entityManager.clear();
    }

    private void conferirFatura(Long faturaId, String total, String pago, StatusFatura status) {
        FaturaDomain fatura = faturaRepository.findById(faturaId).orElseThrow();
        assertThat(fatura.getValorTotal()).isEqualByComparingTo(total);
        assertThat(fatura.getValorPago()).isEqualByComparingTo(pago);
        assertThat(fatura.getStatus()).isEqualTo(status);
    }

    private void conferirContaECartao(String saldo, String limiteUtilizado) {
        assertThat(contaRepository.findById(contaId).orElseThrow().getSaldo()).isEqualByComparingTo(saldo);
        assertThat(cartaoRepository.findById(cartaoId).orElseThrow().getLimiteUtilizado())
                .isEqualByComparingTo(limiteUtilizado);
    }

    private void conferirAjuste(Long compraId, String abatido, String creditado) {
        assertThat(ajusteRepository.findAll()).singleElement().satisfies(ajuste -> {
            assertThat(ajuste.getCompra().getId()).isEqualTo(compraId);
            assertThat(ajuste.getValorAbatido()).isEqualByComparingTo(abatido);
            assertThat(ajuste.getValorCreditado()).isEqualByComparingTo(creditado);
        });
    }

    private List<TransacaoDomain> creditos() {
        return transacaoRepository.findAll().stream()
                .filter(transacao -> transacao.getDescricao() == DescricaoTransacao.CREDITO_CONFIANCA)
                .toList();
    }

    private UserDomain criarUsuario(String email, String nome) {
        UserDomain usuario = new UserDomain();
        usuario.setEmail(email);
        usuario.setUsername(nome);
        usuario.setPasswordHash("hash");
        return userRepository.save(usuario);
    }

    private void autenticar(UserDomain usuario) {
        var autenticacao = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(usuario.getId(), usuario.getEmail(), usuario.getUsername()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    private BigDecimal valor(String quantia) {
        return new BigDecimal(quantia);
    }
}
