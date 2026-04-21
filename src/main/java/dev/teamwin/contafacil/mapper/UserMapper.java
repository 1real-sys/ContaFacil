package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.UserDomain;
import dev.teamwin.contafacil.dto.login.RegisterRequestDTO;
import dev.teamwin.contafacil.dto.user.UserCreateDTO;
import dev.teamwin.contafacil.dto.user.UserResponseDTO;
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
