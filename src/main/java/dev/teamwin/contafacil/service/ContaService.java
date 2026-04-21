package dev.teamwin.contafacil.service;


import dev.teamwin.contafacil.domain.ContaDomain;
import dev.teamwin.contafacil.domain.UserDomain;
import dev.teamwin.contafacil.dto.conta.ContaResponseDTO;
import dev.teamwin.contafacil.mapper.ContaMapper;
import dev.teamwin.contafacil.repository.ContaRepository;
import dev.teamwin.contafacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final ContaMapper contaMapper;

    public ContaResponseDTO abrirConta(){
        UserDomain user  = (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        if(!contaRepository.findByUserId(user.getId()).isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já possui uma conta");
        }

        String contaCorrente = gerarContaCorrente();
        String agencia = gerarAgencia();

        ContaDomain conta = contaMapper.toDomain(contaCorrente, user, agencia);
        conta = contaRepository.save(conta);
        return contaMapper.toResponse(conta);
    }

    private String gerarAgencia(){
        int numero = new Random().nextInt(999) + 1;
        return String.format("%04d", numero);

    }

    private String gerarContaCorrente(){
        int numero = new Random().nextInt(999999) + 1;
        return String.format("%06d", numero);
    }






}
