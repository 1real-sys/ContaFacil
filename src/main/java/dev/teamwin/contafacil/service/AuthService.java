package dev.teamwin.contafacil.service;

import dev.teamwin.contafacil.user.UserDomain;
import dev.teamwin.contafacil.dto.login.LoginRequestDTO;
import dev.teamwin.contafacil.dto.login.RegisterRequestDTO;
import dev.teamwin.contafacil.dto.login.ResponseDTO;
import dev.teamwin.contafacil.infra.security.TokenService;
import dev.teamwin.contafacil.user.UserMapper;
import dev.teamwin.contafacil.user.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String HASH_SENHA_FAKE = new BCryptPasswordEncoder().encode("senha-inexistente-conta-facil");



    public ResponseDTO login(LoginRequestDTO dto) {
        UserDomain user = userRepository.findByEmail(dto.email()).orElse(null);
        boolean senhaCorreta = user != null
                ? passwordEncoder.matches(dto.password(), user.getPasswordHash())
                : passwordEncoder.matches(dto.password(), HASH_SENHA_FAKE);

        if (user == null || !senhaCorreta) {
            log.warn("Tentativa de login falhou para o email: {}", dto.email());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
        String token = tokenService.generateToken(user);
        log.info("Usuário logado com sucesso: {}", user.getEmail());
        return new ResponseDTO(user.getUsername(), token);
    }

    public ResponseDTO registrar(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            log.warn("Tentativa de registro falhou — email já cadastrado: {}", dto.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível completar o cadastro");
        }
        UserDomain domain = userMapper.map(dto);
        domain.setPasswordHash(passwordEncoder.encode(dto.password()));
        domain = userRepository.save(domain);
        log.info("Novo usuário registrado com sucesso: {}", domain.getEmail());
        String token = tokenService.generateToken(domain);
        return new ResponseDTO(domain.getUsername(), token);
    }
}
