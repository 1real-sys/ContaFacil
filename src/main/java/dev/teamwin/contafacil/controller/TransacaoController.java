package dev.teamwin.contafacil.controller;

import dev.teamwin.contafacil.transacao.DepositoRequestDTO;
import dev.teamwin.contafacil.transacao.SaqueRequestDTO;
import dev.teamwin.contafacil.transacao.TedRequestDTO;
import dev.teamwin.contafacil.transacao.TransacaoResponseDTO;
import dev.teamwin.contafacil.transacao.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transacao")
@RequiredArgsConstructor
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping("/depositar")
    public ResponseEntity<TransacaoResponseDTO> depositar(@Valid @RequestBody DepositoRequestDTO dto) {
        return ResponseEntity.ok(transacaoService.depositar(dto));
    }

    @PostMapping("/ted")
    public ResponseEntity<TransacaoResponseDTO> ted(@Valid @RequestBody TedRequestDTO dto) {
        return ResponseEntity.ok(transacaoService.Ted(dto));
    }

    @PostMapping("/saque")
    public ResponseEntity<TransacaoResponseDTO> saque(@Valid @RequestBody SaqueRequestDTO dto) {
        return ResponseEntity.ok(transacaoService.Saque(dto));
    }
}