package dev.teamwin.contafacil.service;


import dev.teamwin.contafacil.domain.UserDomain;
import dev.teamwin.contafacil.dto.login.RegisterRequestDTO;
import dev.teamwin.contafacil.dto.user.UserResponseDTO;
import dev.teamwin.contafacil.mapper.UserMapper;
import dev.teamwin.contafacil.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;




}
