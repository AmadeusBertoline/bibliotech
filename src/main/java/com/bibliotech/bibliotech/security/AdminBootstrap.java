package com.bibliotech.bibliotech.security;

import com.bibliotech.bibliotech.domain.AppUser;
import com.bibliotech.bibliotech.domain.Role;
import com.bibliotech.bibliotech.respository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap.admin.email:}") String email,
                          @Value("${app.bootstrap.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD deve ter ao menos 8 caracteres");
        }

        AppUser admin = new AppUser();
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Usuário ADMIN inicial criado");
    }
}