package com.bibliotech.bibliotech.dto;

import jakarta.validation.constraints.NotNull;

public record LoanRequest(
        @NotNull(message = "O livro é obrigatório") Long bookId,
        @NotNull(message = "O membro é obrigatório") Long memberId
) {}