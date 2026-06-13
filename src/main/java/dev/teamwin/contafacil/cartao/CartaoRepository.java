package dev.teamwin.contafacil.cartao;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartaoRepository extends JpaRepository<CartaoDomain, Long> {


    Optional<CartaoDomain> findByNumeroCartao(String numeroCartao); // nao será cacheado, função auxiliar gerar numero pode conflitar

    @Cacheable(value = "cartoes", key = "#contaId")
    List<CartaoDomain> findByContaId(Long contaId);

    @Override
    @CacheEvict(value = "cartoes", key = "#entity.conta.id")
    <S extends CartaoDomain> S save(S entity);
}
