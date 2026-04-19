package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.CartaoDomain;
import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.StatusCartao;
import dev.teamwin.contafacil.dto.cartao.CartaoCreateDTO;
import dev.teamwin.contafacil.dto.cartao.CartaoDadosSensiveisDTO;
import dev.teamwin.contafacil.dto.cartao.CartaoResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class CartaoMapper {

    public CartaoResponseDTO toResponse(CartaoDomain cartao) {
        return new CartaoResponseDTO(
                cartao.getId(),
                mascararNumero(cartao.getNumeroCartao()),
                cartao.getStatus(),
                cartao.getDataValidade(),
                cartao.getLimiteTotal(),
                cartao.getLimiteUtilizado(),
                cartao.getLimiteDisponivel()
        );
    }

    public CartaoDadosSensiveisDTO toDadosSensiveis(CartaoDomain cartao) {
        return new CartaoDadosSensiveisDTO(
                cartao.getNumeroCartao(),
                cartao.getCvv(),
                cartao.getDataValidade()
        );
    }

    public CartaoDomain toDomain(
            CartaoCreateDTO dto,
            ContaDomain conta,
            String numeroCartao,
            String cvv,
            LocalDateTime dataValidade,
            StatusCartao status
    ) {
        CartaoDomain cartao = new CartaoDomain();
        cartao.setConta(conta);
        cartao.setNumeroCartao(numeroCartao);
        cartao.setCvv(cvv);
        cartao.setDataValidade(dataValidade);
        cartao.setStatus(status);
        cartao.setLimiteTotal(dto.limiteTotal());
        cartao.setLimiteUtilizado(BigDecimal.ZERO.setScale(2));
        return cartao;
    }

    private String mascararNumero(String numeroCartao) {
        if (numeroCartao == null || numeroCartao.length() < 4) {
            return "****";
        }
        String ultimos4 = numeroCartao.substring(numeroCartao.length() - 4);
        return "**** **** **** " + ultimos4;
    }
}
