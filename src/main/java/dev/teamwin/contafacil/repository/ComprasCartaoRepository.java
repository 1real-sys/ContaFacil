package dev.teamwin.contafacil.repository;

import dev.teamwin.contafacil.domain.ComprasCartaoDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ComprasCartaoRepository extends JpaRepository<ComprasCartaoDomain, Long> {

    List<ComprasCartaoDomain> findByFaturaIdOrderByDataCompraDesc(Long faturaId);

    List<ComprasCartaoDomain> findByFaturaCartaoIdAndDataCompraBetweenOrderByDataCompraDesc(
            Long cartaoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    );
}
