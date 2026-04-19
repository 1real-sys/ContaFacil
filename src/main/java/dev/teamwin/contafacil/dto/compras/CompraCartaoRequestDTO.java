package dev.teamwin.contafacil.dto.compras;

import dev.teamwin.contafacil.domain.CategoriaEstabelecimento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompraCartaoRequestDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal valor,

        @NotBlank
        @Size(max = 120)
        String estabelecimento,

        @NotNull
        CategoriaEstabelecimento categoria
) {
}
