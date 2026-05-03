package dev.teamwin.contafacil.user;

import dev.teamwin.contafacil.dto.login.RegisterRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO map(UserDomain user) {
        return new UserResponseDTO(user.getUsername(), user.getEmail());
    }

    public UserDomain map(UserCreateDTO dto) {
        UserDomain user = new UserDomain();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        return user;
    }

    public UserDomain map(RegisterRequestDTO dto) {
        UserDomain user = new UserDomain();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        return user;
    }
}
