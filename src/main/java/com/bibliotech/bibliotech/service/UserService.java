package com.bibliotech.bibliotech.service;

import com.bibliotech.bibliotech.domain.AppUser;
import com.bibliotech.bibliotech.dto.CreateUserRequest;
import com.bibliotech.bibliotech.dto.UserResponse;
import com.bibliotech.bibliotech.exception.ConflictException;
import com.bibliotech.bibliotech.respository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe um usuário com o e-mail " + email);
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());

        AppUser saved = userRepository.save(user);
        log.info("Usuário {} criado com perfil {}", saved.getId(), saved.getRole());
        return UserResponse.from(saved);
    }
}