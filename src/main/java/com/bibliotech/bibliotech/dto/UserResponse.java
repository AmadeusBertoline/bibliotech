package com.bibliotech.bibliotech.dto;

import com.bibliotech.bibliotech.domain.AppUser;
import com.bibliotech.bibliotech.domain.Role;

public record UserResponse(Long id, String email, Role role, Boolean enabled) {

    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRole(), u.getEnabled());
    }
}