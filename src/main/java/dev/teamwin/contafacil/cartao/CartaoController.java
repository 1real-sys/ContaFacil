package dev.teamwin.contafacil.cartao;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cartao")
@RequiredArgsConstructor
public class CartaoController {

    private final CartaoService cartaoService;

    @PostMapping("/emitirCartao")
    public ResponseEntity<CartaoResponseDTO> emitirCartao(@Valid @RequestBody CartaoCreateDTO dto) {
        return ResponseEntity.ok(cartaoService.emitirCartao(dto));
    }

    @PostMapping("/{cartaoId}/desbloquearCartao")
    public ResponseEntity<CartaoResponseDTO> desbloquearCartao(@PathVariable Long cartaoId) {
        return ResponseEntity.ok(cartaoService.ativarCartao(cartaoId));
    }

    @PostMapping("/{cartaoId}/bloquearCartao")
    public ResponseEntity<CartaoResponseDTO> bloquearCartao(@PathVariable Long cartaoId) {
        return ResponseEntity.ok(cartaoService.inativarCartao(cartaoId));
    }

    @PostMapping("/{cartaoId}/solicitarLimite")
    public ResponseEntity<CartaoResponseDTO> solicitarLimite(@PathVariable Long cartaoId){
        return ResponseEntity.ok(cartaoService.solicitarLimite(cartaoId));
    }

    @GetMapping("/meusCartoes")
    public ResponseEntity<List<CartaoResponseDTO>> meusCartoes() {
        return ResponseEntity.ok(cartaoService.listarMeusCartoes());
    }

    @PatchMapping("/{cartaoId}/cancelarCartao")
    public ResponseEntity<String> cancelarCartao(@PathVariable Long cartaoId){
        String mensagem = cartaoService.cancelarCartao(cartaoId);
        return ResponseEntity.ok(mensagem);
    }
}
