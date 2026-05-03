package dev.teamwin.contafacil.transacao;


import dev.teamwin.contafacil.conta.ContaDomain;
import dev.teamwin.contafacil.user.UserDomain;
import dev.teamwin.contafacil.conta.ContaRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoMapper transacaoMapper;
    private final ContaRepository contaRepository;

    @Transactional
    public TransacaoResponseDTO depositar(DepositoRequestDTO dto) {
        UserDomain user = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        ContaDomain conta = contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        BigDecimal saldoAntes = conta.getSaldo();
        BigDecimal saldoDepois = saldoAntes.add(dto.valor());

        conta.setSaldo(saldoDepois);
        contaRepository.save(conta);

        TransacaoDomain transacao = transacaoMapper.fromDepositoRequest(dto, conta, saldoAntes, saldoDepois);
        transacao = transacaoRepository.save(transacao);
        return transacaoMapper.toResponse(transacao);
    }

    @Transactional
    public TransacaoResponseDTO Ted(TedRequestDTO dto){
        UserDomain user = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        ContaDomain contaOrigem = contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (contaOrigem.getContaCorrente().equals(dto.contaDestino())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível transferir para a própria conta");
        }

        if(contaOrigem.getSaldo().compareTo(dto.valor()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        ContaDomain contaDestino = contaRepository.findByContaCorrente(dto.contaDestino())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta destino não encontrada"));

        BigDecimal saldoAntesOrigem = contaOrigem.getSaldo();
        BigDecimal saldoAntesDestino = contaDestino.getSaldo();

        BigDecimal saldoDepoisOrigem = saldoAntesOrigem.subtract(dto.valor());
        BigDecimal saldoDepoisDestino = saldoAntesDestino.add(dto.valor());

        contaOrigem.setSaldo(saldoDepoisOrigem);
        contaDestino.setSaldo(saldoDepoisDestino);
        contaRepository.save(contaOrigem);
        contaRepository.save(contaDestino);

        TransacaoDomain transacaoDestino = transacaoMapper.fromTedRequest(
                dto, contaDestino, contaOrigem, saldoAntesDestino, saldoDepoisDestino);
        transacaoRepository.save(transacaoDestino);

        TransacaoDomain transacaoOrigem = transacaoMapper.fromTedRequest(
                dto, contaOrigem, contaDestino, saldoAntesOrigem, saldoDepoisOrigem);
        transacaoRepository.save(transacaoOrigem);

        return transacaoMapper.toResponse(transacaoOrigem);
    }

    @Transactional
    public TransacaoResponseDTO Saque(SaqueRequestDTO dto){
        UserDomain user = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        ContaDomain contaOrigem = contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));


        if(contaOrigem.getSaldo().compareTo(dto.valor()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        BigDecimal saldoAntesOrigem = contaOrigem.getSaldo();
        BigDecimal saldoDepoisOrigem = saldoAntesOrigem.subtract(dto.valor());

        contaOrigem.setSaldo(saldoDepoisOrigem);
        contaRepository.save(contaOrigem);

        TransacaoDomain transacao = transacaoMapper.fromSaqueRequest(dto, contaOrigem, saldoAntesOrigem, saldoDepoisOrigem);
        transacao = transacaoRepository.save(transacao);
        return transacaoMapper.toResponse(transacao);

    }


}
