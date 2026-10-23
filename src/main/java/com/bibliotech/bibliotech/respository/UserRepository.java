package com.bibliotech.bibliotech.respository;

import com.bibliotech.bibliotech.domain.AppUser;
import com.bibliotech.bibliotech.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
}