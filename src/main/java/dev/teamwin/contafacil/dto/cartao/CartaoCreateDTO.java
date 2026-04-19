package dev.teamwin.contafacil.dto.cartao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CartaoCreateDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal limiteTotal
) {

}
