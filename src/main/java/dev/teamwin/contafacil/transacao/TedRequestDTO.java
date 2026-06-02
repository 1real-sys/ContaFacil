package dev.teamwin.contafacil.transacao;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TedRequestDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @DecimalMax(value = "999999.99", inclusive = true)
        @Digits(integer = 6, fraction = 2)
        BigDecimal valor,

        @Size(max = 255)
        String observacao,

        @NotNull
        String contaDestino
) {
}
