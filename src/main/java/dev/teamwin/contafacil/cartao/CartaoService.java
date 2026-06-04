package dev.teamwin.contafacil.cartao;


import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import dev.teamwin.contafacil.user.UserDomain;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@AllArgsConstructor
@Service
public class CartaoService {

    private static final Logger log = LoggerFactory.getLogger(CartaoService.class);

    private final ContaRepository contaRepository;
    private final CartaoRepository cartaoRepository;
    private final CartaoMapper cartaoMapper;


    @Transactional
    public CartaoResponseDTO emitirCartao(CartaoCreateDTO dto) {
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        boolean temCartaoAtivo = cartaoRepository.findByContaId(conta.getId())
                .stream()
                .anyMatch(c -> c.getStatus() != StatusCartao.CANCELADO);

        if (temCartaoAtivo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta já possui um cartão");
        }

        String numeroCartao = gerarNumeroCartaoUnico(dto.bandeira());
        String cvv = gerarCvv();

        CartaoDomain cartao = cartaoMapper.toDomain(
                dto,
                conta,
                numeroCartao,
                cvv,
                LocalDateTime.now().plusYears(5),
                StatusCartao.INATIVO
        );

        cartao = cartaoRepository.save(cartao);
        log.info("Cartão emitido com sucesso — usuário: {}, bandeira: {}", user.getEmail(), dto.bandeira());
        return cartaoMapper.toResponse(cartao);
    }
    @Transactional
    public CartaoResponseDTO ativarCartao(Long cartaoId){
        UserDomain user = getUsuarioAutenticado();
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
        UserDomain user = getUsuarioAutenticado();
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
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        if (cartao.getLimiteTotal().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Desculpe, sem novas solicitações de limite por enquanto");
        }

        BigDecimal limite = BigDecimal.valueOf(1000 + new Random().nextInt(1501));
        cartao.setLimiteTotal(limite.setScale(2));
        return cartaoMapper.toResponse(cartaoRepository.save(cartao));

    }

    public List<CartaoResponseDTO> listarMeusCartoes() {
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);

        return cartaoRepository.findByContaId(conta.getId())
                .stream()
                .filter(c -> c.getStatus() != StatusCartao.CANCELADO)
                .map(cartaoMapper::toResponse)
                .toList();
    }
    @Transactional
    public String cancelarCartao(Long cartaoId) {
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);

        if (cartao.getStatus() == StatusCartao.ATIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bloqueie o cartão antes de cancelar");
        }

        cartao.setStatus(StatusCartao.CANCELADO);
        cartaoRepository.save(cartao);
        log.info("Cartão cancelado com sucesso — usuário: {}, cartãoId: {}", user.getEmail(), cartaoId);
        return "Cartão cancelado com sucesso";
    }

    public CartaoDadosSensiveisDTO verDadosSensiveis(Long cartaoId) {
        UserDomain user = getUsuarioAutenticado();
        ContaDomain conta = getContaUsuario(user);
        CartaoDomain cartao = getCartaoUsuario(cartaoId, conta);
        return cartaoMapper.toDadosSensiveis(cartao);
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

    private String gerarNumeroCartaoUnico(BandeiraCartao bandeira) {
        String prefixo = bandeira == BandeiraCartao.VISA ? "4" : "5";
        String numero;
        do {
            long parte = (long) (Math.random() * 900_000_000_000_000L) + 100_000_000_000_000L;
            numero = prefixo + parte; // remove o substring(1)
        } while (cartaoRepository.findByNumeroCartao(numero).isPresent());
        return numero;
    }

    private String gerarCvv() {
        return String.format("%03d", new Random().nextInt(1000));
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
