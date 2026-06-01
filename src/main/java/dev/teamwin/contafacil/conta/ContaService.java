package dev.teamwin.contafacil.conta;


import dev.teamwin.contafacil.cartao.CartaoRepository;
import dev.teamwin.contafacil.cartao.StatusCartao;
import dev.teamwin.contafacil.fatura.FaturaRepository;
import dev.teamwin.contafacil.fatura.StatusFatura;
import java.math.BigDecimal;

import dev.teamwin.contafacil.service.AuthService;
import dev.teamwin.contafacil.user.UserDomain;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final ContaMapper contaMapper;
    private final CartaoRepository cartaoRepository;
    private final FaturaRepository faturaRepository;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);


    public ContaResponseDTO abrirConta(){
        UserDomain user  = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        if(!contaRepository.findByUserId(user.getId()).isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já possui uma conta");
        }

        String contaCorrente = gerarContaCorrente();
        String agencia = gerarAgencia();

        ContaDomain conta = contaMapper.toDomain(contaCorrente, user, agencia);
        conta = contaRepository.save(conta);
        log.info("Nova conta criada com sucesso para o usuário: {}", user.getEmail());
        return contaMapper.toResponse(conta);
    }

    private String gerarAgencia(){
        int numero = new Random().nextInt(999) + 1;
        return String.format("%04d", numero);

    }

    private String gerarContaCorrente(){
        String contaCorrente;
        do {
            int numero = new Random().nextInt(999999) + 1;
            contaCorrente = String.format("%06d", numero);
        } while (contaRepository.findByContaCorrente(contaCorrente).isPresent());
        return contaCorrente;
    }


    public ContaResponseDTO minhaConta(){
        UserDomain user = (UserDomain) SecurityContextHolder.getContext().
                getAuthentication()
                .getPrincipal();
        return contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .map(contaMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    public SaldoResponseDTO consultarSaldo(){
        UserDomain user = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        ContaDomain conta = contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
                return contaMapper.toSaldoResponse(conta);
    }

    public String encerrarConta() {
        UserDomain user = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        ContaDomain conta = contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (conta.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta possui saldo. Realize o saque antes de encerrar");
        }

        boolean temCartaoAtivo = cartaoRepository.findByContaId(conta.getId())
                .stream()
                .anyMatch(c -> c.getStatus() != StatusCartao.CANCELADO);

        if (temCartaoAtivo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancele todos os cartões antes de encerrar a conta");
        }

        boolean temFaturaEmAberto = faturaRepository.findByCartaoIdAndStatus(
                        cartaoRepository.findByContaId(conta.getId())
                                .stream()
                                .findFirst()
                                .map(c -> c.getId())
                                .orElse(-1L),
                        StatusFatura.ABERTA)
                .stream()
                .anyMatch(f -> f.getValorPendente().compareTo(BigDecimal.ZERO) > 0);

        if (temFaturaEmAberto) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Possui fatura em aberto. Quite todas as faturas antes de encerrar");
        }

        contaRepository.delete(conta);
        return "Conta encerrada com sucesso";
    }


}
