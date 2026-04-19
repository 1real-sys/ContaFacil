package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.TransacaoDomain;
import dev.teamwin.contafacil.dto.extrato.ExtratoItemDTO;
import dev.teamwin.contafacil.dto.extrato.ExtratoResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class ExtratoMapper {

    public ExtratoItemDTO toItem(TransacaoDomain transacao) {
        return new ExtratoItemDTO(
                transacao.getId(),
                transacao.getDataTransacao(),
                transacao.getDescricao(),
                transacao.getValor(),
                transacao.getSaldoAntes(),
                transacao.getSaldoDepois(),
                transacao.getObservacao()
        );
    }

    public ExtratoResponseDTO toResponse(
            ContaDomain conta,
            LocalDate dataInicio,
            LocalDate dataFim,
            BigDecimal saldoInicialPeriodo,
            BigDecimal saldoFinalPeriodo,
            List<TransacaoDomain> transacoes
    ) {
        List<ExtratoItemDTO> itens = transacoes.stream().map(this::toItem).toList();
        return new ExtratoResponseDTO(
                conta.getId(),
                conta.getContaCorrente(),
                conta.getAgencia(),
                dataInicio,
                dataFim,
                saldoInicialPeriodo,
                saldoFinalPeriodo,
                itens
        );
    }
}
