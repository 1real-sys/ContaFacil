package dev.teamwin.contafacil.transacao;

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
