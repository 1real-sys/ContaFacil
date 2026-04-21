package dev.teamwin.contafacil.dto.conta;

import java.math.BigDecimal;

public record ContaResponseDTO(String contaCorrente, String agencia, BigDecimal saldo) {
}
