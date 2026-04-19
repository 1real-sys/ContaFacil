package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.DescricaoTransacao;
import dev.teamwin.contafacil.domain.TransacaoDomain;
import dev.teamwin.contafacil.dto.transacao.DepositoRequestDTO;
import dev.teamwin.contafacil.dto.transacao.SaqueRequestDTO;
import dev.teamwin.contafacil.dto.transacao.TedRequestDTO;
import dev.teamwin.contafacil.dto.transacao.TransacaoResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransacaoMapper {

    public TransacaoResponseDTO toResponse(TransacaoDomain transacao) {
        return new TransacaoResponseDTO(
                transacao.getValor(),
                transacao.getDescricao(),
                transacao.getObservacao(),
                transacao.getDataTransacao()
        );
    }

    public TransacaoDomain fromDepositoRequest(
            DepositoRequestDTO dto,
            ContaDomain conta,
            BigDecimal saldoAntes,
            BigDecimal saldoDepois
    ) {
        TransacaoDomain transacao = buildBase(conta, saldoAntes, saldoDepois, dto.valor(), dto.observacao());
        transacao.setDescricao(DescricaoTransacao.DEPOSITO);
        return transacao;
    }

    public TransacaoDomain fromSaqueRequest(
            SaqueRequestDTO dto,
            ContaDomain conta,
            BigDecimal saldoAntes,
            BigDecimal saldoDepois
    ) {
        TransacaoDomain transacao = buildBase(conta, saldoAntes, saldoDepois, dto.valor(), dto.observacao());
        transacao.setDescricao(DescricaoTransacao.SAQUE);
        return transacao;
    }

    public TransacaoDomain fromTedRequest(
            TedRequestDTO dto,
            ContaDomain contaOrigem,
            ContaDomain contaDestino,
            BigDecimal saldoAntes,
            BigDecimal saldoDepois
    ) {
        TransacaoDomain transacao = buildBase(contaOrigem, saldoAntes, saldoDepois, dto.valor(), dto.observacao());
        transacao.setDescricao(DescricaoTransacao.TED);
        transacao.setContaDestino(contaDestino);
        return transacao;
    }

    private TransacaoDomain buildBase(
            ContaDomain conta,
            BigDecimal saldoAntes,
            BigDecimal saldoDepois,
            BigDecimal valor,
            String observacao
    ) {
        TransacaoDomain transacao = new TransacaoDomain();
        transacao.setConta(conta);
        transacao.setSaldoAntes(saldoAntes);
        transacao.setSaldoDepois(saldoDepois);
        transacao.setValor(valor);
        transacao.setObservacao(observacao);
        return transacao;
    }
}
