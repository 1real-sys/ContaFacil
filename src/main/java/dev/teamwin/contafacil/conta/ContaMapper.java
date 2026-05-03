package dev.teamwin.contafacil.conta;

import dev.teamwin.contafacil.user.UserDomain;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ContaMapper {

    public ContaResponseDTO toResponse(ContaDomain conta) {
        String nome = conta.getUser() != null ? conta.getUser().getUsername() : null;
        Long idUsuario = conta.getUser() != null ? conta.getUser().getId() : null;
        return new ContaResponseDTO( conta.getContaCorrente(),
                conta.getAgencia(),
                conta.getSaldo());
    }

    public SaldoResponseDTO toSaldoResponse(ContaDomain conta) {
        return new SaldoResponseDTO(conta.getSaldo());
    }

    public ContaDomain toDomain(String contaCorrente, UserDomain user, String agencia) {
        ContaDomain conta = new ContaDomain();
        conta.setContaCorrente(contaCorrente);
        conta.setAgencia(agencia);
        conta.setUser(user);
        conta.setSaldo(BigDecimal.ZERO.setScale(2));
        return conta;
    }
}
