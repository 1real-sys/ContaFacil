package dev.teamwin.contafacil.conta;

import java.math.BigDecimal;

public record ContaResponseDTO(String contaCorrente, String agencia, BigDecimal saldo) {
}
