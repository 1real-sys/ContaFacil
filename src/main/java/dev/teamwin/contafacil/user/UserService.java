package dev.teamwin.contafacil.user;


import dev.teamwin.contafacil.infra.security.AuthenticatedUser;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);



    public UserResponseDTO meuPerfil(){
        AuthenticatedUser principal = getUsuarioAutenticado();
        return new UserResponseDTO(principal.getUsername(), principal.getEmail());
    }

    @Transactional
    public UserResponseDTO atualizarNome(AtualizarNomeRequestDTO dto){
        AuthenticatedUser principal = getUsuarioAutenticado();
        UserDomain user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        user.setUsername(dto.username());
        user = userRepository.save(user);
        log.info("Nome atualizado para: {} — usuário: {}", dto.username(), user.getEmail());
        return userMapper.map(user);

    }

    private AuthenticatedUser getUsuarioAutenticado(){
        return (AuthenticatedUser) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }




}
