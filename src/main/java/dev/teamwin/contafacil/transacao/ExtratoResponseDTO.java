package dev.teamwin.contafacil.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExtratoResponseDTO(
        Long contaId,
        String contaCorrente,
        String agencia,
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal saldoInicialPeriodo,
        BigDecimal saldoFinalPeriodo,
        List<ExtratoItemDTO> itens
) {
}
