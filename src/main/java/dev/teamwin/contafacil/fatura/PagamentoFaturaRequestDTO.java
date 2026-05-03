package dev.teamwin.contafacil.fatura;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoFaturaRequestDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal valor
) {
}
