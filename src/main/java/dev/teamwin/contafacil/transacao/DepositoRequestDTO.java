package dev.teamwin.contafacil.transacao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepositoRequestDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal valor,

        @Size(max = 255)
        String observacao
) {
}
