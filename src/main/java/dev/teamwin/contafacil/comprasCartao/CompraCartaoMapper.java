package dev.teamwin.contafacil.comprasCartao;

import dev.teamwin.contafacil.fatura.FaturaDomain;
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
