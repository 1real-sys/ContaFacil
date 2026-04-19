package dev.teamwin.contafacil.repository;

import dev.teamwin.contafacil.domain.DescricaoTransacao;
import dev.teamwin.contafacil.domain.TransacaoDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<TransacaoDomain, Long> {

    List<TransacaoDomain> findByContaIdOrderByDataTransacaoDesc(Long contaId);

    List<TransacaoDomain> findByContaIdAndDataTransacaoBetweenOrderByDataTransacaoDesc(
            Long contaId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    );

    List<TransacaoDomain> findByContaIdAndDescricaoAndDataTransacaoBetweenOrderByDataTransacaoDesc(
            Long contaId,
            DescricaoTransacao descricao,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    );

    Optional<TransacaoDomain> findTopByContaIdAndDataTransacaoBeforeOrderByDataTransacaoDesc(
            Long contaId,
            LocalDateTime dataReferencia
    );
}
