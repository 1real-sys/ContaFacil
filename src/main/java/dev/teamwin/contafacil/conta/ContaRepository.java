package dev.teamwin.contafacil.conta;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<ContaDomain, Long> {

    Optional<ContaDomain> findByContaCorrente(String contaCorrente);

    @Override
    @CacheEvict(value = "contas", key = "#entity.user.id")
    <S extends ContaDomain> S save(S entity);

    @Cacheable(value = "contas", key = "#userId")
    List<ContaDomain> findByUserId(Long userId);
}
