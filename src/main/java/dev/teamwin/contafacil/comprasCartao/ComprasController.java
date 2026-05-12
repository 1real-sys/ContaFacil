package dev.teamwin.contafacil.comprasCartao;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
public class ComprasController {

    private final ComprasService comprasService;


    @PostMapping("/{cartaoId}/lancar")
    public ResponseEntity<CompraCartaoResponseDTO> lancarCompra(
            @PathVariable Long cartaoId,
            @Valid @RequestBody CompraCartaoRequestDTO dto) {
        return ResponseEntity.ok(comprasService.lancarCompra(cartaoId, dto));
    }

    @PatchMapping("/{compraId}/cancelar")
    public ResponseEntity<CompraCartaoResponseDTO> cancelarCompra(@PathVariable Long compraId) {
        return ResponseEntity.ok(comprasService.cancelarCompra(compraId));
    }
}
