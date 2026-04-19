package dev.teamwin.contafacil.dto.compras;

import dev.teamwin.contafacil.domain.CategoriaEstabelecimento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComprasCartaoDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal valor,

        @NotNull
        LocalDateTime dataCompra,

        @NotBlank
        @Size(max = 120)
        String estabelecimento,

        @NotBlank
        @Pattern(regexp = "\\d{4}")
        String ultimos4Digitos,

        @NotNull
        CategoriaEstabelecimento categoria
) {
}
