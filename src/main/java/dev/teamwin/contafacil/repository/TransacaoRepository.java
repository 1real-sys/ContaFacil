package dev.teamwin.contafacil.repository;

import dev.teamwin.contafacil.domain.DescricaoTransacao;
import dev.teamwin.contafacil.domain.TransacaoDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<TransacaoDomain, Long> {

    @Query("SELECT t FROM TransacaoDomain t WHERE t.conta.id = :contaId AND t.dataTransacao BETWEEN :inicio AND :fim ORDER BY t.dataTransacao DESC")
    List<TransacaoDomain> findExtrato(
            @Param("contaId") Long contaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT t FROM TransacaoDomain t WHERE t.conta.id = :contaId AND t.descricao = :tipo AND t.dataTransacao BETWEEN :inicio AND :fim ORDER BY t.dataTransacao DESC")
    List<TransacaoDomain> findExtratoPorTipo(
            @Param("contaId") Long contaId,
            @Param("tipo") DescricaoTransacao tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    Optional<TransacaoDomain> findTopByContaIdAndDataTransacaoBeforeOrderByDataTransacaoDesc(
            @Param("contaId") Long contaId,
            @Param("dataReferencia") LocalDateTime dataReferencia);
}
