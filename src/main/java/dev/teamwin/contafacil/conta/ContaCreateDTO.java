package dev.teamwin.contafacil.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ContaCreateDTO(
        @NotBlank
        @Size(max = 100)
        String nome,

        @NotBlank
        @Size(max = 20)
        String contaCorrente,

        @NotNull
        @Positive
        Long idUsuario
) {
}
