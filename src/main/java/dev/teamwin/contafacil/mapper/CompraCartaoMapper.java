package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.ComprasCartaoDomain;
import dev.teamwin.contafacil.domain.FaturaDomain;
import dev.teamwin.contafacil.dto.compras.CompraCartaoRequestDTO;
import dev.teamwin.contafacil.dto.compras.CompraCartaoResponseDTO;
import dev.teamwin.contafacil.dto.compras.ComprasCartaoDTO;
import org.springframework.stereotype.Component;

@Component
public class CompraCartaoMapper {

    public CompraCartaoResponseDTO toResponse(ComprasCartaoDomain compra) {
        return new CompraCartaoResponseDTO(
                compra.getId(),
                compra.getValor(),
                compra.getDataCompra(),
                compra.getEstabelecimento(),
                compra.getUltimos4Digitos(),
                compra.getCategoria(),
                compra.getFatura() != null ? compra.getFatura().getId() : null
        );
    }

    public ComprasCartaoDTO toDto(ComprasCartaoDomain compra) {
        return new ComprasCartaoDTO(
                compra.getValor(),
                compra.getDataCompra(),
                compra.getEstabelecimento(),
                compra.getUltimos4Digitos(),
                compra.getCategoria()
        );
    }

    public ComprasCartaoDomain toDomain(CompraCartaoRequestDTO dto, FaturaDomain fatura, String ultimos4Digitos) {
        ComprasCartaoDomain compra = new ComprasCartaoDomain();
        compra.setValor(dto.valor());
        compra.setEstabelecimento(dto.estabelecimento());
        compra.setCategoria(dto.categoria());
        compra.setUltimos4Digitos(ultimos4Digitos);
        compra.setFatura(fatura);
        return compra;
    }
}
