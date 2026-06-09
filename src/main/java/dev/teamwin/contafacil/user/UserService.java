package dev.teamwin.contafacil.user;


import dev.teamwin.contafacil.cartao.CartaoService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);



    public UserResponseDTO meuPerfil(){
        UserDomain user = getUsuarioAutenticado();
        return userMapper.map(user);
    }

    @Transactional
    public UserResponseDTO atualizarNome(AtualizarNomeRequestDTO dto){
        UserDomain user = getUsuarioAutenticado();
        user.setUsername(dto.username());
        user = userRepository.save(user);
        log.info("Nome atualizado para: {} — usuário: {}", dto.username(), user.getEmail());
        return userMapper.map(user);

    }

    private UserDomain getUsuarioAutenticado(){
        return (UserDomain) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }




}
