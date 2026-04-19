package dev.teamwin.contafacil.dto.transacao;

import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.DescricaoTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoResponseDTO(BigDecimal valor, DescricaoTransacao descricao, String observacao, LocalDateTime dataTransacao) {
}
