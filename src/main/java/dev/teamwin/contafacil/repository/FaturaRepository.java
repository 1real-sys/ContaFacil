package dev.teamwin.contafacil.repository;

import dev.teamwin.contafacil.domain.FaturaDomain;
import dev.teamwin.contafacil.domain.StatusFatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaturaRepository extends JpaRepository<FaturaDomain, Long> {

    Optional<FaturaDomain> findByCartaoIdAndAnoAndMes(Long cartaoId, Integer ano, Integer mes);

    List<FaturaDomain> findByCartaoIdOrderByAnoDescMesDesc(Long cartaoId);

    List<FaturaDomain> findByCartaoIdAndStatus(Long cartaoId, StatusFatura status);
}
