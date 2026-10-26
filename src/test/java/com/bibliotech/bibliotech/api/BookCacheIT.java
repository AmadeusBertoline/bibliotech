package com.bibliotech.bibliotech.api;

import com.bibliotech.bibliotech.domain.MemberStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookCacheIT extends AbstractApiIT {

    @Test
    void findById_isServedFromCache_untilEvicted() throws Exception {
        long bookId = createBook("Titulo Original", 1);
        send(get("/books/{id}", bookId), librarianToken())
                .andExpect(jsonPath("$.title").value("Titulo Original"));

        // altera direto no banco, por fora da aplicação: se vier o título antigo, veio do cache
        jdbc.update("UPDATE books SET title = 'Alterado por fora' WHERE id = ?", bookId);

        send(get("/books/{id}", bookId), librarianToken())
                .andExpect(jsonPath("$.title").value("Titulo Original"));
    }

    @Test
    void loanAndReturn_invalidateCachedStock() throws Exception {
        long bookId = createBook(1);
        assertThat(availableCopies(bookId)).isEqualTo(1);       // popula o cache

        long loanId = openLoan(bookId, createMember(MemberStatus.ACTIVE));
        assertThat(availableCopies(bookId)).isZero();           // não pode vir 1 do cache

        send(post("/loans/{id}/return", loanId), librarianToken()).andExpect(status().isOk());
        assertThat(availableCopies(bookId)).isEqualTo(1);       // idem na devolução
    }
}