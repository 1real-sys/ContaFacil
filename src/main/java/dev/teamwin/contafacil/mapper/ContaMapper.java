package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.UserDomain;
import dev.teamwin.contafacil.dto.conta.ContaCreateDTO;
import dev.teamwin.contafacil.dto.conta.ContaResponseDTO;
import dev.teamwin.contafacil.dto.conta.SaldoResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ContaMapper {

    public ContaResponseDTO toResponse(ContaDomain conta) {
        String nome = conta.getUser() != null ? conta.getUser().getUsername() : null;
        Long idUsuario = conta.getUser() != null ? conta.getUser().getId() : null;
        return new ContaResponseDTO(nome, conta.getContaCorrente(), idUsuario);
    }

    public SaldoResponseDTO toSaldoResponse(ContaDomain conta) {
        return new SaldoResponseDTO(conta.getSaldo());
    }

    public ContaDomain toDomain(ContaCreateDTO dto, UserDomain user, String agencia) {
        ContaDomain conta = new ContaDomain();
        conta.setContaCorrente(dto.contaCorrente());
        conta.setAgencia(agencia);
        conta.setUser(user);
        conta.setSaldo(BigDecimal.ZERO.setScale(2));
        return conta;
    }
}
