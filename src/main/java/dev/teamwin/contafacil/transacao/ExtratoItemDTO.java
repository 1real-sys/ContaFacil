package dev.teamwin.contafacil.transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExtratoItemDTO(
        Long transacaoId,
        LocalDateTime dataTransacao,
        DescricaoTransacao tipo,
        BigDecimal valor,
        BigDecimal saldoAntes,
        BigDecimal saldoDepois,
        String observacao
) {
}
