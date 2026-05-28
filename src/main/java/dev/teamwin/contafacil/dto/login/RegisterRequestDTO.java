package dev.teamwin.contafacil.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO (@NotBlank String username,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 10)
                                  @Pattern(
                                          regexp = "^(?=.*[0-9])(?=.*[A-Z]).+$",
                                          message = "A senha deve conter ao menos 1 número e 1 letra maiúscula"
                                  )String password) {

}
