package dev.teamwin.contafacil.cartao;

import java.time.LocalDateTime;

public record CartaoDadosSensiveisDTO(String numeroCartaoOculto, LocalDateTime dataValidade) {
}
