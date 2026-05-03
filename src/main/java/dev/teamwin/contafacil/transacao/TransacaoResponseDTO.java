package dev.teamwin.contafacil.transacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoResponseDTO(BigDecimal valor, DescricaoTransacao descricao, String observacao, LocalDateTime dataTransacao, BigDecimal saldoAntes,
                                   BigDecimal saldoDepois) {
}
