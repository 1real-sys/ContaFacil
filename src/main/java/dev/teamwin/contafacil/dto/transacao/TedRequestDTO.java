package dev.teamwin.contafacil.dto.transacao;

import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.DescricaoTransacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TedRequestDTO(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        BigDecimal valor,

        @Size(max = 255)
        String observacao,

        @NotNull
        String contaDestino
) {
}
