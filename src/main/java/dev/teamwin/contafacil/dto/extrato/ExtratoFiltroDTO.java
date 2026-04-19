package dev.teamwin.contafacil.dto.extrato;

import dev.teamwin.contafacil.domain.DescricaoTransacao;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record ExtratoFiltroDTO(
        @PastOrPresent
        LocalDate dataInicio,

        @PastOrPresent
        LocalDate dataFim,

        DescricaoTransacao tipo
) {
}
