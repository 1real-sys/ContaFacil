package dev.teamwin.contafacil.fatura;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faturas")
@RequiredArgsConstructor
public class FaturaController {

    private final FaturaService faturaService;

    @GetMapping("/{cartaoId}/atual")
    public ResponseEntity<FaturaResponseDTO> consultarFaturaAtual(@PathVariable Long cartaoId){
        return ResponseEntity.ok(faturaService.consultarFaturaAtual(cartaoId));
    }

    @GetMapping("/{cartaoId}/historico")
    public ResponseEntity<List<FaturaResponseDTO>> listarFaturas(@PathVariable Long cartaoId){
        return ResponseEntity.ok(faturaService.listarFaturas(cartaoId));
    }

    @PostMapping("/{faturaId}/pagar")
    public ResponseEntity<FaturaResponseDTO> pagarFatura(
            @PathVariable Long faturaId,
            @Valid @RequestBody PagamentoFaturaRequestDTO dto) {
        return ResponseEntity.ok(faturaService.pagarFatura(faturaId, dto));
    }




}
