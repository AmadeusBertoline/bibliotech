package com.bibliotech.bibliotech.dto;

import com.bibliotech.bibliotech.domain.MemberStatus;
import jakarta.validation.constraints.*;

public record MemberRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        String phone,

        MemberStatus status   // opcional; se nulo, ACTIVE na criação e mantém o atual na atualização
) {}