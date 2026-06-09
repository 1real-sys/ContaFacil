package dev.teamwin.contafacil.user;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/perfil")
    public ResponseEntity<UserResponseDTO> meuPerfil() {
        return ResponseEntity.ok(userService.meuPerfil());
    }

    @PatchMapping("/atualizarNome")
    public ResponseEntity<UserResponseDTO> atualizarNome(@Valid @RequestBody AtualizarNomeRequestDTO dto) {
        return ResponseEntity.ok(userService.atualizarNome(dto));
    }
}
