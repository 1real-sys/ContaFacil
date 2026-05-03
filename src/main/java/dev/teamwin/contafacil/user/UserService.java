package dev.teamwin.contafacil.user;


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
