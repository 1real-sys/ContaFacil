package dev.teamwin.contafacil.cartao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CartaoCreateDTO(
        @NotNull
        BandeiraCartao bandeira
) {

}
