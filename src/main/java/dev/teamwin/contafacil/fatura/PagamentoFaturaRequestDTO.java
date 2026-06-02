package dev.teamwin.contafacil.fatura;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoFaturaRequestDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @DecimalMax(value = "999999.99", inclusive = true)
        @Digits(integer = 6, fraction = 2)
        BigDecimal valor
) {
}
