package dev.teamwin.contafacil.comprasCartao;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CompraCartaoRequestDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @DecimalMax(value = "999999.99", inclusive = true)
        @Digits(integer = 6, fraction = 2)
        BigDecimal valor,

        @NotBlank
        @Size(max = 120)
        String estabelecimento,

        @NotNull
        CategoriaEstabelecimento categoria
) {
}
