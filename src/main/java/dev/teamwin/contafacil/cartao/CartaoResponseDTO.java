package dev.teamwin.contafacil.cartao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartaoResponseDTO(Long id, String numeroCartaoOculto, StatusCartao status, LocalDateTime dataValidade, BigDecimal limiteTotal, BigDecimal limiteUtilizado, BigDecimal limiteDisponivel, BandeiraCartao bandeira) {
}
