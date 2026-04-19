package dev.teamwin.contafacil.dto.cartao;

import java.time.LocalDateTime;

public record CartaoDadosSensiveisDTO(String numeroCartao, String cvv, LocalDateTime dataValidade) {
}
