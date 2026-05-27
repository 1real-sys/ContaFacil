package dev.teamwin.contafacil.service;

import dev.teamwin.contafacil.user.UserDomain;
import dev.teamwin.contafacil.dto.login.LoginRequestDTO;
import dev.teamwin.contafacil.dto.login.RegisterRequestDTO;
import dev.teamwin.contafacil.dto.login.ResponseDTO;
import dev.teamwin.contafacil.infra.security.TokenService;
import dev.teamwin.contafacil.user.UserMapper;
import dev.teamwin.contafacil.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@AllArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;


    public ResponseDTO login(LoginRequestDTO dto) {
        UserDomain domain = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        if (!passwordEncoder.matches(dto.password(), domain.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
        String token = tokenService.generateToken(domain);
        return new ResponseDTO(domain.getUsername(), token);
    }

    public ResponseDTO registrar(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível completar o cadastro");
        }
        UserDomain domain = userMapper.map(dto);
        domain.setPasswordHash(passwordEncoder.encode(dto.password()));
        domain = userRepository.save(domain);
        String token = tokenService.generateToken(domain);
        return new ResponseDTO(domain.getUsername(), token);
    }
}
