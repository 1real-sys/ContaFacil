package dev.teamwin.contafacil.cartao;


import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.fatura.FaturaRepository;
import dev.teamwin.contafacil.infra.security.AuthenticatedUser;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Service
public class CartaoService {

    private static final Logger log = LoggerFactory.getLogger(CartaoService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ContaRepository contaRepository;
    private final CartaoRepository cartaoRepository;
    private final CartaoMapper cartaoMapper;
    private final FaturaRepository faturaRepository;


    @Transactional
    public CartaoResponseDTO emitirCartao(CartaoCreateDTO dto) {
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        boolean temCartaoAtivo = cartaoRepository.findByContaId(conta.getId())
                .stream()
                .anyMatch(c -> c.getStatus() != StatusCartao.CANCELADO);

        if (temCartaoAtivo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta já possui um cartão");
        }

        String numeroCartao = gerarNumeroCartaoUnico(dto.bandeira());

        CartaoDomain cartao = cartaoMapper.toDomain(
                dto,
                conta,
                numeroCartao,
                LocalDateTime.now().plusYears(5),
                StatusCartao.INATIVO
        );

        cartao = cartaoRepository.save(cartao);
        log.info("Cartão emitido com sucesso — usuário: {}, bandeira: {}", user.getEmail(), dto.bandeira());
        return cartaoMapper.toResponse(cartao);
    }
    @Transactional
    public CartaoResponseDTO ativarCartao(Long cartaoId){
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);
        if (cartao.getStatus() != StatusCartao.INATIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cartão não está inativo");
        }
        cartao.setStatus(StatusCartao.ATIVO);
        CartaoDomain cartaoSalvo = cartaoRepository.save(cartao);
        log.info("Cartão ativado com sucesso — usuário: {}, cartãoId: {}", user.getEmail(), cartaoId);
        return cartaoMapper.toResponse(cartaoSalvo);
    }
    @Transactional
    public CartaoResponseDTO inativarCartao(Long cartaoId){
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);
        if (cartao.getStatus() != StatusCartao.ATIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cartão não está ativo");
        }
        cartao.setStatus(StatusCartao.INATIVO);
        CartaoDomain cartaoSalvo = cartaoRepository.save(cartao);
        log.info("Cartão bloqueado com sucesso — usuário: {}, cartãoId: {}", user.getEmail(), cartaoId);
        return cartaoMapper.toResponse(cartaoSalvo);
    }
    @Transactional
    public CartaoResponseDTO solicitarLimite(Long cartaoId){
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        if (cartao.getLimiteTotal().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Desculpe, sem novas solicitações de limite por enquanto");
        }

        BigDecimal limite = BigDecimal.valueOf(1000 + RANDOM.nextInt(1501));
        cartao.setLimiteTotal(limite.setScale(2));
        return cartaoMapper.toResponse(cartaoRepository.save(cartao));

    }

    public List<CartaoResponseDTO> listarMeusCartoes() {
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        return cartaoRepository.findByContaId(conta.getId())
                .stream()
                .filter(c -> c.getStatus() != StatusCartao.CANCELADO)
                .map(cartaoMapper::toResponse)
                .toList();
    }
    @Transactional
    public String cancelarCartao(Long cartaoId) {
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        if (cartao.getStatus() == StatusCartao.ATIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bloqueie o cartão antes de cancelar");
        }

        boolean temFaturaAberta = faturaRepository
                .findByCartaoIdOrderByAnoDescMesDesc(cartaoId)
                .stream()
                .anyMatch(f -> f.getValorPendente().compareTo(BigDecimal.ZERO) > 0);

        if(temFaturaAberta) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quite todas as faturas pendentes antes de cancelar o cartão");
        }

        cartao.setStatus(StatusCartao.CANCELADO);
        cartaoRepository.save(cartao);
        log.info("Cartão cancelado com sucesso — usuário: {}, cartãoId: {}", user.getEmail(), cartaoId);
        return "Cartão cancelado com sucesso";
    }

    public CartaoDadosSensiveisDTO verDadosSensiveis(Long cartaoId) {
        AuthenticatedUser user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);
        return cartaoMapper.toDadosSensiveis(cartao);
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

    private String gerarNumeroCartaoUnico(BandeiraCartao bandeira) {
        String prefixo = bandeira == BandeiraCartao.VISA ? "4" : "5";
        String numero;
        do {
            StringBuilder corpo = new StringBuilder(prefixo);
            for (int i = 0; i < 14; i++) {
                corpo.append(RANDOM.nextInt(10));
            }
            numero = corpo.append(luhnCheckDigit(corpo.toString())).toString();
        } while (cartaoRepository.findByNumeroCartao(numero).isPresent());
        return numero;
    }

    private static String luhnCheckDigit(String base) {
        int soma = 0;
        boolean duplicar = true;
        for (int i = base.length() - 1; i >= 0; i--) {
            int digito = base.charAt(i) - '0';
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            soma += digito;
            duplicar = !duplicar;
        }
        return Integer.toString((10 - (soma % 10)) % 10);
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
