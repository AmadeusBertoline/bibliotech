package com.bibliotech.bibliotech.api;

import com.bibliotech.bibliotech.domain.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookApiIT extends AbstractApiIT {

    @Test
    void createBook_returns201WithLocation_andFullStock() throws Exception {
        send(jsonRequest(HttpMethod.POST, "/books", bookBody("978-1111111111", 3)), librarianToken())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.totalCopies").value(3))
                .andExpect(jsonPath("$.availableCopies").value(3));
    }

    @Test
    void createBook_withInvalidFields_returns400WithFieldErrors() throws Exception {
        Map<String, Object> invalid = Map.of(
                "title", "", "isbn", "abc", "author", "", "publishedYear", 1, "totalCopies", 0);

        send(jsonRequest(HttpMethod.POST, "/books", invalid), librarianToken())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.errors[?(@.field=='isbn')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='totalCopies')]").exists());
    }

    @Test
    void createBook_withDuplicateIsbn_returns409() throws Exception {
        Map<String, Object> body = bookBody("978-2222222222", 1);
        send(jsonRequest(HttpMethod.POST, "/books", body), librarianToken()).andExpect(status().isCreated());

        send(jsonRequest(HttpMethod.POST, "/books", body), librarianToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"));
    }

    @Test
    void search_filtersByTitleAndAvailability() throws Exception {
        long lentOut = createBook("Raro Emprestado", 1);
        createBook("Raro Disponivel", 1);
        openLoan(lentOut, createMember(MemberStatus.ACTIVE));

        send(get("/books?title=raro"), librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        send(get("/books?title=raro&available=true"), librarianToken())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Raro Disponivel"));

        send(get("/books?title=raro&available=false"), librarianToken())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Raro Emprestado"));
    }

    @Test
    void update_cannotShrinkTotalBelowLentCopies_returns422() throws Exception {
        long bookId = createBook(3);
        openLoan(bookId, createMember(MemberStatus.ACTIVE));
        openLoan(bookId, createMember(MemberStatus.ACTIVE));    // 2 emprestados, 1 disponível

        Map<String, Object> shrink = Map.of(
                "title", "Livro", "isbn", "978-3333333333", "author", "Autor",
                "publishedYear", 2020, "totalCopies", 1);

        send(jsonRequest(HttpMethod.PUT, "/books/" + bookId, shrink), librarianToken())
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getUnknownBook_returns404ProblemDetail() throws Exception {
        send(get("/books/999999"), librarianToken())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    // ---------- comportamento do handler global ----------

    @Test
    void unknownRoute_returns404_notFiveHundred() throws Exception {
        send(get("/nao-existe"), librarianToken()).andExpect(status().isNotFound());
    }

    @Test
    void wrongHttpMethod_returns405() throws Exception {
        send(jsonRequest(HttpMethod.PATCH, "/books/1", Map.of()), librarianToken())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/books")
                .contentType("application/json").content("{ isto não é json"), librarianToken())
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSortProperty_returns400() throws Exception {
        send(get("/books?sort=campoQueNaoExiste"), librarianToken()).andExpect(status().isBadRequest());
    }

    @Test
    void pageSize_isCappedAtOneHundred() throws Exception {
        send(get("/books?size=1000"), librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    private static Map<String, Object> bookBody(String isbn, int copies) {
        return Map.of("title", "Effective Java", "isbn", isbn, "author", "Joshua Bloch",
                "publishedYear", 2018, "totalCopies", copies);
    }
}