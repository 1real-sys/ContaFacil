package dev.teamwin.contafacil.mapper;

import dev.teamwin.contafacil.domain.UserDomain;
import dev.teamwin.contafacil.dto.user.UserCreateDTO;
import dev.teamwin.contafacil.dto.user.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO toResponse(UserDomain user) {
        return new UserResponseDTO(user.getUsername(), user.getEmail());
    }

    public UserDomain toDomain(UserCreateDTO dto, String passwordHash) {
        UserDomain user = new UserDomain();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordHash);
        return user;
    }
}
