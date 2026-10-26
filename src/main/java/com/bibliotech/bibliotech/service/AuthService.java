package com.bibliotech.bibliotech.service;

import com.bibliotech.bibliotech.domain.AppUser;
import com.bibliotech.bibliotech.dto.LoginRequest;
import com.bibliotech.bibliotech.dto.TokenResponse;
import com.bibliotech.bibliotech.exception.InvalidCredentialsException;
import com.bibliotech.bibliotech.respository.UserRepository;
import com.bibliotech.bibliotech.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("timing-equalizer");
    }

    public TokenResponse login(LoginRequest req) {
        AppUser user = userRepository.findByEmail(req.email().trim().toLowerCase()).orElse(null);

        // Sempre executa o BCrypt, mesmo sem usuário, para não vazar por tempo de
        // resposta se o e-mail existe
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(req.password(), hash);

        if (user == null || !matches || !user.getEnabled()) {
            log.warn("Falha de login: credenciais inválidas");
            throw new InvalidCredentialsException();
        }

        log.info("Login realizado: userId={}", user.getId());
        return tokenService.generate(user);
    }
}