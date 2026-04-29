package dev.teamwin.contafacil.service;

import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.DescricaoTransacao;
import dev.teamwin.contafacil.domain.TransacaoDomain;
import dev.teamwin.contafacil.domain.UserDomain;
import dev.teamwin.contafacil.dto.extrato.ExtratoResponseDTO;
import dev.teamwin.contafacil.mapper.ExtratoMapper;
import dev.teamwin.contafacil.repository.ContaRepository;
import dev.teamwin.contafacil.repository.TransacaoRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Service
@AllArgsConstructor
public class ExtratoService {

    private final ContaRepository contaRepository;
    private final ExtratoMapper extratoMapper;
    private final TransacaoRepository transacaoRepository;



    public ExtratoResponseDTO extrato(LocalDate dataInicio, LocalDate dataFim, DescricaoTransacao tipo) {
        UserDomain user = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        ContaDomain conta = contaRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(23, 59, 59);

        List<TransacaoDomain> transacoes;
        if (tipo != null) {
            transacoes = transacaoRepository.findExtratoPorTipo(conta.getId(), tipo, inicio, fim);
        } else {
            transacoes = transacaoRepository.findExtrato(conta.getId(), inicio, fim);
        }


        if (dataInicio.isAfter(dataFim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data início não pode ser maior que data fim");
        }

        BigDecimal saldoInicialPeriodo = transacaoRepository
                .findTopByContaIdAndDataTransacaoBeforeOrderByDataTransacaoDesc(conta.getId(), inicio)
                .map(TransacaoDomain::getSaldoDepois)
                .orElse(BigDecimal.ZERO);

        BigDecimal saldoFinalPeriodo = transacoes.isEmpty()
                ? saldoInicialPeriodo
                : transacoes.get(0).getSaldoDepois();

        return extratoMapper.toResponse(conta, dataInicio, dataFim, saldoInicialPeriodo, saldoFinalPeriodo, transacoes);
    }
}
