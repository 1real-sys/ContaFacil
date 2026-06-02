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
import dev.teamwin.contafacil.service.AuthService;
import dev.teamwin.contafacil.user.UserDomain;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);


    @Transactional
    public CompraCartaoResponseDTO lancarCompra(Long cartaoId, CompraCartaoRequestDTO dto){
        UserDomain user = getUsuarioAutenticado();
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
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        ComprasCartaoDomain compra = comprasCartaoRepository.findById(compraId)
                .filter(c -> c.getFatura().getCartao().getConta().getId().equals(conta.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra não encontrada"));

        if (compra.getStatus() == StatusCompra.CANCELADA){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compra já está cancelada");
        }

        CartaoDomain cartao = compra.getFatura().getCartao();
        cartao.setLimiteUtilizado(cartao.getLimiteUtilizado().subtract(compra.getValor()));
        cartaoRepository.save(cartao);

        FaturaDomain fatura = compra.getFatura();
        fatura.setValorTotal(fatura.getValorTotal().subtract(compra.getValor()));
        faturaRepository.save(fatura);

        compra.setStatus(StatusCompra.CANCELADA);
        log.info("Compra cancelada com sucesso para o usuário: {}, valor: {}", user.getEmail(), compra.getValor());
        return compraCartaoMapper.toResponse(comprasCartaoRepository.save(compra));
    }




    /**
     * Estorna uma compra de uma fatura já paga.
     *
     * Este mét0do foi criado para corrigir um bug não previsto no fluxo original:
     * o cancelarCompra() só funcionava para faturas com status ABERTA. Quando o
     * cliente pagava a fatura e depois tentava cancelar uma compra, ocorria erro
     * interno pois as regras de negócio são diferentes — em uma fatura paga, o
     * dinheiro já saiu da conta do cliente e precisa ser devolvido via estorno.
     *
     * Diferença entre cancelar e estornar:
     * - Cancelar: fatura ABERTA, compra não foi cobrada, apenas devolve o limite.
     * - Estornar: fatura PAGA, compra já foi cobrada, devolve o valor ao saldo.
     */
    @Transactional
    public CompraCartaoResponseDTO estornarCompra(Long compraId) {
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        ComprasCartaoDomain compra = comprasCartaoRepository.findById(compraId)
                .filter(c -> c.getFatura().getCartao().getConta().getId().equals(conta.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compra não encontrada"));

        FaturaDomain fatura = compra.getFatura();
        if(fatura.getStatus() != StatusFatura.PAGA){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estorno disponível apenas para faturas pagas. Para faturas abertas, cancele a compra.");
        }
        if(compra.getStatus() == StatusCompra.CANCELADA){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compra já está cancelada, não é possível estornar");
        }

        BigDecimal valorEstorno = compra.getValor();
        fatura.setValorTotal(fatura.getValorTotal().subtract(valorEstorno));

        BigDecimal novoValorPago = fatura.getValorPago().subtract(valorEstorno);
        fatura.setValorPago(novoValorPago.max(BigDecimal.ZERO)); //nunca fica negativo

        if(fatura.getValorPago().compareTo(fatura.getValorTotal()) < 0){
            fatura.setStatus(StatusFatura.ABERTA);
        }

        faturaRepository.save(fatura);

        conta.setSaldo(conta.getSaldo().add(valorEstorno));
        contaRepository.save(conta);

        compra.setStatus(StatusCompra.CANCELADA);
        log.info("Compra estornada com sucesso para o usuário: {}, valor: {}", user.getEmail(), compra.getValor());

        return compraCartaoMapper.toResponse(comprasCartaoRepository.save(compra));
    }



    private UserDomain getUsuarioAutenticado(){
        return (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
    private ContaDomain getContaUsuario(UserDomain user){
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
