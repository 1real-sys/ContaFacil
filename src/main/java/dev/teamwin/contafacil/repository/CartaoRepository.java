package dev.teamwin.contafacil.repository;

import dev.teamwin.contafacil.domain.CartaoDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartaoRepository extends JpaRepository<CartaoDomain, Long> {

    Optional<CartaoDomain> findByNumeroCartao(String numeroCartao);

    List<CartaoDomain> findByContaId(Long contaId);
}
