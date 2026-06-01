package dev.teamwin.contafacil.fatura;


import dev.teamwin.contafacil.cartao.CartaoDomain;
import dev.teamwin.contafacil.cartao.CartaoRepository;
import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.user.UserDomain;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FaturaService {

    private static final Logger log = LoggerFactory.getLogger(FaturaService.class);

    private final FaturaRepository faturaRepository;
    private final CartaoRepository cartaoRepository;
    private final ContaRepository contaRepository;
    private final FaturaMapper faturaMapper;


    public FaturaDomain obterOuCriarFaturaCompetencia(CartaoDomain cartao){
        LocalDateTime agora = LocalDateTime.now();
        int ano = agora.getYear();
        int mes = agora.getMonthValue();

        return faturaRepository.findByCartaoIdAndAnoAndMes(cartao.getId(), ano, mes)
                .orElseGet(() -> {
                    FaturaDomain fatura = new FaturaDomain();
                    fatura.setCartao(cartao);
                    fatura.setAno(ano);
                    fatura.setMes(mes);
                    fatura.setDataFechamento(LocalDate.of(ano, mes, 6).plusMonths(1));
                    fatura.setDataVencimento(LocalDate.of(ano, mes, 6).plusMonths(1));
                    fatura.setValorTotal(BigDecimal.ZERO.setScale(2));
                    fatura.setValorPago(BigDecimal.ZERO.setScale(2));
                    fatura.setStatus(StatusFatura.ABERTA);
                    return faturaRepository.save(fatura);
                });

    }

    public FaturaResponseDTO consultarFaturaAtual(Long cartaoId){
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        LocalDateTime agora = LocalDateTime.now();
        FaturaDomain fatura = faturaRepository
                .findByCartaoIdAndAnoAndMes(cartao.getId(), agora.getYear(), agora.getMonthValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma fatura encontrada para o cartão neste mês"));

        return faturaMapper.toResponse(fatura);
    }

   public List<FaturaResponseDTO> listarFaturas(Long cartaoId){
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        return faturaRepository.findByCartaoIdOrderByAnoDescMesDesc(cartao.getId())
                .stream()
                .map(faturaMapper::toResponse)
                .toList();
   }

    public FaturaResponseDTO pagarFatura(Long cartaoId, PagamentoFaturaRequestDTO dto) {
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        LocalDateTime agora = LocalDateTime.now();
        FaturaDomain fatura = faturaRepository
                .findByCartaoIdAndAnoAndMes(cartao.getId(), agora.getYear(), agora.getMonthValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma fatura encontrada"));

        if (fatura.getStatus() == StatusFatura.PAGA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fatura já está paga");
        }

        if (dto.valor().compareTo(fatura.getValorPendente()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor maior que o pendente da fatura");
        }
        if (conta.getSaldo().compareTo(dto.valor()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente para pagamento");
        }

        conta.setSaldo(conta.getSaldo().subtract(dto.valor()));
        contaRepository.save(conta);

        fatura.setValorPago(fatura.getValorPago().add(dto.valor()));

        cartao.setLimiteUtilizado(cartao.getLimiteUtilizado().subtract(dto.valor()));
        cartaoRepository.save(cartao);

        if (fatura.getValorPendente().compareTo(BigDecimal.ZERO) == 0) {
            fatura.setStatus(StatusFatura.PAGA);
        }

        FaturaDomain faturaSalva = faturaRepository.save(fatura);
        log.info("Pagamento de fatura realizado — usuário: {}, cartão: {}, valor: R$ {}", user.getEmail(), cartaoId, dto.valor());
        return faturaMapper.toResponse(faturaSalva);
    }



    private UserDomain getUsuarioAutenticado() {
        return (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private ContaDomain getContaUsuario(UserDomain user) {
        return contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    private CartaoDomain getCartaoUsuario(Long cartaoId, ContaDomain conta) {
        return cartaoRepository.findById(cartaoId)
                .filter(c -> c.getConta().getId().equals(conta.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cartão não encontrado"));
    }
}
