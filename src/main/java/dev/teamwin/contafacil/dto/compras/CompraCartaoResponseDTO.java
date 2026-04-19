package dev.teamwin.contafacil.dto.compras;

import dev.teamwin.contafacil.domain.CategoriaEstabelecimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraCartaoResponseDTO(
        Long id,
        BigDecimal valor,
        LocalDateTime dataCompra,
        String estabelecimento,
        String ultimos4Digitos,
        CategoriaEstabelecimento categoria,
        Long faturaId
) {
}
