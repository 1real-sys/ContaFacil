package dev.teamwin.contafacil.infra.security;

import dev.teamwin.contafacil.BaseIntegrationTest;
import dev.teamwin.contafacil.cartao.BandeiraCartao;
import dev.teamwin.contafacil.cartao.CartaoDomain;
import dev.teamwin.contafacil.cartao.CartaoRepository;
import dev.teamwin.contafacil.cartao.StatusCartao;
import dev.teamwin.contafacil.comprasCartao.AjusteCompraRepository;
import dev.teamwin.contafacil.comprasCartao.CategoriaEstabelecimento;
import dev.teamwin.contafacil.comprasCartao.ComprasCartaoDomain;
import dev.teamwin.contafacil.comprasCartao.ComprasCartaoRepository;
import dev.teamwin.contafacil.comprasCartao.StatusCompra;
import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.fatura.FaturaDomain;
import dev.teamwin.contafacil.fatura.FaturaRepository;
import dev.teamwin.contafacil.fatura.StatusFatura;
import dev.teamwin.contafacil.transacao.TransacaoRepository;
import dev.teamwin.contafacil.user.UserDomain;
import dev.teamwin.contafacil.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class IsolamentoUsuariosIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private ContaRepository contaRepository;
    @Autowired private CartaoRepository cartaoRepository;
    @Autowired private FaturaRepository faturaRepository;
    @Autowired private ComprasCartaoRepository compraRepository;
    @Autowired private AjusteCompraRepository ajusteRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private EntityManager entityManager;

    private String tokenX;
    private String tokenY;
    private Long contaXId;
    private Long contaYId;
    private Long cartaoYId;
    private Long faturaYId;
    private Long compraYId;

    @BeforeEach
    void prepararUsuarios() {
        UserDomain x = criarUsuario("x@isolamento.test", "usuario-x");
        UserDomain y = criarUsuario("y@isolamento.test", "usuario-y");
        tokenX = tokenService.generateToken(x);
        tokenY = tokenService.generateToken(y);

        ContaDomain contaX = criarConta(x, "900001", "1000.00");
        ContaDomain contaY = criarConta(y, "900002", "1000.00");
        contaXId = contaX.getId();
        contaYId = contaY.getId();

        CartaoDomain cartaoY = new CartaoDomain();
        cartaoY.setConta(contaY);
        cartaoY.setNumeroCartao("4000000000000002");
        cartaoY.setBandeira(BandeiraCartao.VISA);
        cartaoY.setStatus(StatusCartao.ATIVO);
        cartaoY.setDataValidade(LocalDateTime.now().plusYears(2));
        cartaoY.setLimiteTotal(new BigDecimal("1000.00"));
        cartaoY.setLimiteUtilizado(new BigDecimal("100.00"));
        cartaoYId = cartaoRepository.save(cartaoY).getId();

        LocalDate hoje = LocalDate.now();
        FaturaDomain faturaY = new FaturaDomain();
        faturaY.setCartao(cartaoY);
        faturaY.setAno(hoje.getYear());
        faturaY.setMes(hoje.getMonthValue());
        faturaY.setDataFechamento(hoje.plusMonths(1));
        faturaY.setDataVencimento(hoje.plusMonths(1));
        faturaY.setValorTotal(new BigDecimal("100.00"));
        faturaY.setValorPago(new BigDecimal("50.00"));
        faturaY.setStatus(StatusFatura.ABERTA);
        faturaYId = faturaRepository.save(faturaY).getId();

        ComprasCartaoDomain compraY = new ComprasCartaoDomain();
        compraY.setFatura(faturaY);
        compraY.setValor(new BigDecimal("100.00"));
        compraY.setEstabelecimento("Compra privada de Y");
        compraY.setUltimos4Digitos("0002");
        compraY.setCategoria(CategoriaEstabelecimento.SHOPPING);
        compraY.setStatus(StatusCompra.AUTORIZADA);
        compraYId = compraRepository.save(compraY).getId();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void usuarioXNaoLeCartaoFaturaOuCompraDeY() throws Exception {
        mvc.perform(get("/cartao/meusCartoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/cartao/meusCartoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cartaoYId));

        mvc.perform(get("/cartao/{id}/dadosSensiveis", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/faturas/{id}/atual", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/faturas/{id}/historico", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/faturas/{id}/atual", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compras[0].id").value(compraYId));
    }

    @Test
    void usuarioXNaoAlteraCartaoFaturaOuCompraDeY() throws Exception {
        mvc.perform(post("/cartao/{id}/solicitarLimite", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/cartao/{id}/bloquearCartao", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/cartao/{id}/desbloquearCartao", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/cartao/{id}/cancelarCartao", cartaoYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/compras/{id}/lancar", cartaoYId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10,\"estabelecimento\":\"Teste\",\"categoria\":\"SHOPPING\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/faturas/{id}/pagar", faturaYId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/compras/{id}/cancelar", compraYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/compras/{id}/estornar", compraYId).header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isNotFound());

        assertThat(cartaoRepository.findById(cartaoYId).orElseThrow().getStatus()).isEqualTo(StatusCartao.ATIVO);
        assertThat(faturaRepository.findById(faturaYId).orElseThrow().getValorPago()).isEqualByComparingTo("50.00");
        assertThat(compraRepository.findById(compraYId).orElseThrow().getStatus()).isEqualTo(StatusCompra.AUTORIZADA);
        assertThat(contaRepository.findById(contaYId).orElseThrow().getSaldo()).isEqualByComparingTo("1000.00");
        assertThat(ajusteRepository.count()).isZero();
        assertThat(transacaoRepository.count()).isZero();
    }

    @Test
    void perfilContaEExtratoUsamIdentidadeDoToken() throws Exception {
        mvc.perform(get("/user/perfil").header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("x@isolamento.test"));
        mvc.perform(get("/conta/minhaConta").header(HttpHeaders.AUTHORIZATION, bearer(tokenX)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contaCorrente").value("900001"));

        // Um ID de outra conta no corpo não muda o destino do depósito.
        mvc.perform(post("/transacao/depositar").header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":10,\"contaId\":" + contaYId + "}"))
                .andExpect(status().isOk());
        assertThat(contaRepository.findById(contaXId).orElseThrow().getSaldo()).isEqualByComparingTo("1010.00");
        assertThat(contaRepository.findById(contaYId).orElseThrow().getSaldo()).isEqualByComparingTo("1000.00");

        String hoje = LocalDate.now().toString();
        mvc.perform(get("/conta/extrato").header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .param("dataInicio", hoje).param("dataFim", hoje))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contaId").value(contaXId))
                .andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(get("/conta/extrato").header(HttpHeaders.AUTHORIZATION, bearer(tokenY))
                        .param("dataInicio", hoje).param("dataFim", hoje))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contaId").value(contaYId))
                .andExpect(jsonPath("$.itens").isEmpty());
    }

    @Test
    void semTokenOuComTokenAdulteradoNaoAcessaDados() throws Exception {
        mvc.perform(get("/cartao/{id}/dadosSensiveis", cartaoYId))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/cartao/{id}/dadosSensiveis", cartaoYId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenY + "adulterado")))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/faturas/{id}/pagar", faturaYId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":10}"))
                .andExpect(status().is4xxClientError());
        assertThat(faturaRepository.findById(faturaYId).orElseThrow().getValorPago()).isEqualByComparingTo("50.00");
    }

    @Test
    void camposDeIdentidadeNoCorpoNaoPermitemAlterarY() throws Exception {
        mvc.perform(patch("/user/atualizarNome").header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"novo-x\",\"id\":" + contaYId
                                + ",\"email\":\"y@isolamento.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("novo-x"))
                .andExpect(jsonPath("$.email").value("x@isolamento.test"));
        mvc.perform(get("/user/perfil").header(HttpHeaders.AUTHORIZATION, bearer(tokenY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("usuario-y"));
    }

    @Test
    void valoresInvalidosNaoAlteramSaldoOuFatura() throws Exception {
        mvc.perform(post("/transacao/depositar").header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":-100}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/transacao/saque").header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/transacao/ted").header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":-10,\"contaDestino\":\"900002\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/faturas/{id}/pagar", faturaYId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenY))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":-10}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/transacao/depositar").header(HttpHeaders.AUTHORIZATION, bearer(tokenX))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":"))
                .andExpect(status().isBadRequest());

        assertThat(contaRepository.findById(contaXId).orElseThrow().getSaldo()).isEqualByComparingTo("1000.00");
        assertThat(contaRepository.findById(contaYId).orElseThrow().getSaldo()).isEqualByComparingTo("1000.00");
        assertThat(faturaRepository.findById(faturaYId).orElseThrow().getValorPago()).isEqualByComparingTo("50.00");
        assertThat(transacaoRepository.count()).isZero();
    }

    private UserDomain criarUsuario(String email, String nome) {
        UserDomain user = new UserDomain();
        user.setEmail(email);
        user.setUsername(nome);
        user.setPasswordHash("hash-de-teste");
        return userRepository.save(user);
    }

    private ContaDomain criarConta(UserDomain user, String numero, String saldo) {
        ContaDomain conta = new ContaDomain();
        conta.setUser(user);
        conta.setContaCorrente(numero);
        conta.setAgencia("0001");
        conta.setSaldo(new BigDecimal(saldo));
        return contaRepository.save(conta);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
