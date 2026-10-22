package com.bibliotech.bibliotech.dto;

import jakarta.validation.constraints.*;

public record BookRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
        String title,

        @NotBlank(message = "O ISBN é obrigatório")
        @Pattern(regexp = "^[0-9-]{10,17}$", message = "ISBN inválido")
        String isbn,

        @NotBlank(message = "O autor é obrigatório")
        @Size(max = 150, message = "O autor deve ter no máximo 150 caracteres")
        String author,

        @NotNull(message = "O ano de publicação é obrigatório")
        @Min(value = 1000, message = "Ano de publicação inválido")
        @Max(value = 2100, message = "Ano de publicação inválido")
        Integer publishedYear,

        @NotNull(message = "O total de exemplares é obrigatório")
        @Min(value = 1, message = "Deve haver pelo menos 1 exemplar")
        Integer totalCopies
) {}