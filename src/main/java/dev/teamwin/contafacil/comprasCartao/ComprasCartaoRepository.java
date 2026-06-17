package dev.teamwin.contafacil.comprasCartao;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ComprasCartaoRepository extends JpaRepository<ComprasCartaoDomain, Long> {


    @Cacheable(value = "comprasCartao", key = "#faturaId")
    @EntityGraph(attributePaths = "fatura")
    List<ComprasCartaoDomain> findByFaturaIdOrderByDataCompraDesc(Long faturaId);

    List<ComprasCartaoDomain> findByFaturaCartaoIdAndDataCompraBetweenOrderByDataCompraDesc(
            Long cartaoId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    );

    @Override
    @CacheEvict(value = "comprasCartao", key = "#entity.fatura.id")
    <S extends ComprasCartaoDomain> S save(S entity);
}
