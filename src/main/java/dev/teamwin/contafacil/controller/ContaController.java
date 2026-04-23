package dev.teamwin.contafacil.controller;


import dev.teamwin.contafacil.dto.conta.ContaResponseDTO;
import dev.teamwin.contafacil.dto.conta.SaldoResponseDTO;
import dev.teamwin.contafacil.service.ContaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conta")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;

     @PostMapping("/abrirConta")
     public ResponseEntity<ContaResponseDTO> abrirConta(){
         return ResponseEntity.ok(contaService.abrirConta());
     }

    @GetMapping("/minhaConta")
    public ResponseEntity<ContaResponseDTO> minhaConta() {
        return ResponseEntity.ok(contaService.minhaConta());
    }

    @GetMapping("/saldo")
    public ResponseEntity<SaldoResponseDTO> consultarSaldo() {
        return ResponseEntity.ok(contaService.consultarSaldo());
    }

}
