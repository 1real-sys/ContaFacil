package dev.teamwin.contafacil.comprasCartao;


import dev.teamwin.contafacil.cartao.CartaoDomain;
import dev.teamwin.contafacil.cartao.CartaoRepository;
import dev.teamwin.contafacil.cartao.StatusCartao;
import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.fatura.FaturaDomain;
import dev.teamwin.contafacil.fatura.FaturaRepository;
import dev.teamwin.contafacil.fatura.FaturaService;
import dev.teamwin.contafacil.fatura.StatusFatura;
import dev.teamwin.contafacil.infra.security.AuthenticatedUser;
import dev.teamwin.contafacil.transacao.DescricaoTransacao;
import dev.teamwin.contafacil.transacao.TransacaoDomain;
import dev.teamwin.contafacil.transacao.TransacaoRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ComprasService {

    private final ContaRepository contaRepository;
    private final CartaoRepository cartaoRepository;
    private final CompraCartaoMapper compraCartaoMapper;
    private final ComprasCartaoRepository comprasCartaoRepository;
    private final FaturaService faturaService;
    private final FaturaRepository faturaRepository;
    private final AjusteCompraRepository ajusteCompraRepository;
    private  final TransacaoRepository transacaoRepository;

    private static final Logger log = LoggerFactory.getLogger(ComprasService.class);


    @Transactional
    public CompraCartaoResponseDTO lancarCompra(Long cartaoId, CompraCartaoRequestDTO dto){
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        validarCartaoAtivoEValido(cartao);
        validarLimiteDisponivel(cartao, dto.valor());

        FaturaDomain fatura = faturaService.obterOuCriarFaturaCompetencia(cartao);

        String ultimos4 = cartao.getNumeroCartao().substring(cartao.getNumeroCartao().length() - 4);

        ComprasCartaoDomain compra = compraCartaoMapper.toDomain(dto, fatura, ultimos4);
        compra.setStatus(StatusCompra.AUTORIZADA);
        comprasCartaoRepository.save(compra);

        cartao.setLimiteUtilizado(cartao.getLimiteUtilizado().add(dto.valor()));
        cartaoRepository.save(cartao);

        fatura.setValorTotal(fatura.getValorTotal().add(dto.valor()));

        if (fatura.getStatus() == StatusFatura.PAGA){
            fatura.setStatus(StatusFatura.ABERTA);
        }
        faturaRepository.save(fatura);

        return compraCartaoMapper.toResponse(compra);
    }
    @Transactional
    public CompraCartaoResponseDTO cancelarCompra(Long compraId){
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        ComprasCartaoDomain compra = comprasCartaoRepository.findById(compraId)
                .filter(c -> c.getFatura().getCartao().getConta().getId().equals(conta.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra não encontrada"));

        if (compra.getStatus() == StatusCompra.CANCELADA){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compra já está cancelada");
        }

        FaturaDomain fatura = compra.getFatura();
        if (fatura.getStatus() != StatusFatura.ABERTA){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cancelamento disponível apenas para faturas em aberto. Use estorno para faturas pagas.");
        }
        if (fatura.getValorPago().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fatura já possui pagamento; use o estorno");
        }


        CartaoDomain cartao = compra.getFatura().getCartao();
        cartao.setLimiteUtilizado(cartao.getLimiteUtilizado().subtract(compra.getValor()));
        cartaoRepository.save(cartao);

        fatura.setValorTotal(fatura.getValorTotal().subtract(compra.getValor()));
        faturaRepository.save(fatura);

        compra.setStatus(StatusCompra.CANCELADA);
        log.info("Compra cancelada com sucesso para o usuário: {}, valor: {}", user.getEmail(), compra.getValor());
        return compraCartaoMapper.toResponse(comprasCartaoRepository.save(compra));
    }

    @Transactional
    public CompraCartaoResponseDTO estornarCompra(Long compraId){
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        ComprasCartaoDomain compra = comprasCartaoRepository.findById(compraId)
                .filter(c -> c.getFatura().getCartao().getConta().getId().equals(conta.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra não encontrada"));

        if (compra.getStatus() == StatusCompra.CANCELADA){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compra já está cancelada");
        }

        FaturaDomain fatura = compra.getFatura();

        if (fatura.getValorPago().compareTo(BigDecimal.ZERO) == 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fatura sem pagamento; Use o cancelamento comum");
        }
        BigDecimal valorAbatido = compra.getValor().min(fatura.getValorPendente());
        BigDecimal valorCreditado = compra.getValor().subtract(valorAbatido);

        CartaoDomain cartao = fatura.getCartao();

        // Só a parte ainda pendente sai da fatura e libera limite.
        fatura.setValorTotal(fatura.getValorTotal().subtract(valorAbatido));
        cartao.setLimiteUtilizado(cartao.getLimiteUtilizado().subtract(valorAbatido));

        if (fatura.getValorPendente().compareTo(BigDecimal.ZERO) == 0) {
            fatura.setStatus(StatusFatura.PAGA);
        }

        // A parte já paga volta para a conta e aparece no extrato.
        if (valorCreditado.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal saldoAntes = conta.getSaldo();
            conta.setSaldo(saldoAntes.add(valorCreditado));

            TransacaoDomain transacao = new TransacaoDomain();
            transacao.setConta(conta);
            transacao.setDescricao(DescricaoTransacao.CREDITO_CONFIANCA);
            transacao.setValor(valorCreditado);
            transacao.setSaldoAntes(saldoAntes);
            transacao.setSaldoDepois(conta.getSaldo());
            transacao.setObservacao("Cancelamento da compra " + compra.getId());

            contaRepository.save(conta);
            transacaoRepository.save(transacao);
        }

        // Guarda quanto foi abatido e quanto virou crédito.
        AjusteCompraDomain ajuste = new AjusteCompraDomain();
        ajuste.setCompra(compra);
        ajuste.setValorAbatido(valorAbatido);
        ajuste.setValorCreditado(valorCreditado);

        compra.setStatus(StatusCompra.CANCELADA);

        cartaoRepository.save(cartao);
        faturaRepository.save(fatura);
        comprasCartaoRepository.save(compra);
        ajusteCompraRepository.save(ajuste);

        return compraCartaoMapper.toResponse(compra);
    }




    private AuthenticatedUser getUsuarioAutenticado(){
        return (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
    private ContaDomain getContaUsuario(AuthenticatedUser user){
        return contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

    }
    private CartaoDomain getCartaoUsuario(Long cartaoId, ContaDomain conta){
        return cartaoRepository.findById(cartaoId)
                .filter(cartao -> cartao.getConta().getId().equals(conta.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartão não encontrado"));
    }

    private void validarCartaoAtivoEValido(CartaoDomain cartao) {
        if (cartao.getStatus() != StatusCartao.ATIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cartão não está ativo");
        }
        if (cartao.getDataValidade().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cartão expirado");
        }
    }

    private void validarLimiteDisponivel(CartaoDomain cartao, BigDecimal valor) {
        if (cartao.getLimiteDisponivel().compareTo(valor) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite insuficiente");
        }
    }
}
