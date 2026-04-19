package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.FaturaDomain;
import dev.teamwin.contafacil.dto.compras.CompraCartaoResponseDTO;
import dev.teamwin.contafacil.dto.fatura.FaturaResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class FaturaMapper {

    private final CompraCartaoMapper compraCartaoMapper;

    public FaturaMapper(CompraCartaoMapper compraCartaoMapper) {
        this.compraCartaoMapper = compraCartaoMapper;
    }

    public FaturaResponseDTO toResponse(FaturaDomain fatura) {
        List<CompraCartaoResponseDTO> compras = fatura.getCompras() == null
                ? Collections.emptyList()
                : fatura.getCompras().stream().map(compraCartaoMapper::toResponse).toList();

        return new FaturaResponseDTO(
                fatura.getId(),
                fatura.getAno(),
                fatura.getMes(),
                fatura.getDataFechamento(),
                fatura.getDataVencimento(),
                fatura.getValorTotal(),
                fatura.getValorPago(),
                fatura.getValorPendente(),
                fatura.getStatus(),
                compras
        );
    }
}
