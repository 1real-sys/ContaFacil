package dev.teamwin.contafacil.dto.fatura;

import dev.teamwin.contafacil.domain.StatusFatura;
import dev.teamwin.contafacil.dto.compras.CompraCartaoResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FaturaResponseDTO(
        Long id,
        Integer ano,
        Integer mes,
        LocalDate dataFechamento,
        LocalDate dataVencimento,
        BigDecimal valorTotal,
        BigDecimal valorPago,
        BigDecimal valorPendente,
        StatusFatura status,
        List<CompraCartaoResponseDTO> compras
) {
}
