package dev.teamwin.contafacil.controller;


import dev.teamwin.contafacil.domain.DescricaoTransacao;
import dev.teamwin.contafacil.dto.conta.ContaResponseDTO;
import dev.teamwin.contafacil.dto.conta.SaldoResponseDTO;
import dev.teamwin.contafacil.dto.extrato.ExtratoResponseDTO;
import dev.teamwin.contafacil.service.ContaService;
import dev.teamwin.contafacil.service.ExtratoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/conta")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;
    private final ExtratoService extratoService;

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

    @GetMapping("/extrato")
    public ResponseEntity<ExtratoResponseDTO> extrato(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) DescricaoTransacao tipo
    ) {
        return ResponseEntity.ok(extratoService.extrato(dataInicio, dataFim, tipo));
    }

}
