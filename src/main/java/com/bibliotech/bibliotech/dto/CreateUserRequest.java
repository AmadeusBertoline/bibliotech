package com.bibliotech.bibliotech.dto;

import com.bibliotech.bibliotech.domain.Role;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String password,

        @NotNull(message = "O perfil é obrigatório")
        Role role
) {}