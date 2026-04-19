package dev.teamwin.contafacil.repository;

import dev.teamwin.contafacil.domain.ContaDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<ContaDomain, Long> {

    Optional<ContaDomain> findByContaCorrente(String contaCorrente);

    List<ContaDomain> findByUserId(Long userId);
}
