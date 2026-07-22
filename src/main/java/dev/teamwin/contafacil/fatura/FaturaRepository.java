package dev.teamwin.contafacil.fatura;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaturaRepository extends JpaRepository<FaturaDomain, Long> {

    @Cacheable(value = "faturas", key = "#cartaoId + '-' + #ano + '-' + #mes",
               unless = "#result == null || !#result.present")
    @EntityGraph(attributePaths = "compras")
    Optional<FaturaDomain> findByCartaoIdAndAnoAndMes(Long cartaoId, Integer ano, Integer mes);

    @Cacheable(value = "faturas", key = "#cartaoId")
    List<FaturaDomain> findByCartaoIdOrderByAnoDescMesDesc(Long cartaoId);

    List<FaturaDomain> findByCartaoIdAndStatus(Long cartaoId, StatusFatura status);

    @Override
    @Caching(evict = {
        @CacheEvict(value = "faturas", key = "#entity.cartao.id + '-' + #entity.ano + '-' + #entity.mes"),
        @CacheEvict(value = "faturas", key = "#entity.cartao.id")
    })
    <S extends FaturaDomain> S save(S entity);
}
