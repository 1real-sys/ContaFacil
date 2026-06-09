package dev.teamwin.contafacil.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarNomeRequestDTO(
        @NotBlank
        @Size(max = 50)
        String username
) {}
