package com.bibliotech.bibliotech.api;

import com.bibliotech.bibliotech.domain.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MemberApiIT extends AbstractApiIT {

    @Test
    void createMember_defaultsToActive() throws Exception {
        send(jsonRequest(HttpMethod.POST, "/members",
                Map.of("name", "Ana Souza", "email", "ana@test.local")), librarianToken())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createMember_withInvalidEmail_returns400() throws Exception {
        send(jsonRequest(HttpMethod.POST, "/members",
                Map.of("name", "Ana", "email", "nao-e-email")), librarianToken())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists());
    }

    @Test
    void createMember_withDuplicateEmail_returns409() throws Exception {
        Map<String, String> body = Map.of("name", "Ana", "email", "repetido@test.local");
        send(jsonRequest(HttpMethod.POST, "/members", body), librarianToken()).andExpect(status().isCreated());

        send(jsonRequest(HttpMethod.POST, "/members", body), librarianToken()).andExpect(status().isConflict());
    }

    @Test
    void update_canSuspendMember() throws Exception {
        long id = createMember(MemberStatus.ACTIVE);

        send(jsonRequest(HttpMethod.PUT, "/members/" + id,
                Map.of("name", "Nome Novo", "email", "novo-email@test.local", "status", "SUSPENDED")),
                librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void memberLoans_listsOnlyThatMembersLoans_andUnknownMemberIs404() throws Exception {
        long memberId = createMember(MemberStatus.ACTIVE);
        long otherId = createMember(MemberStatus.ACTIVE);
        openLoan(createBook(2), memberId);
        openLoan(createBook(2), memberId);
        openLoan(createBook(2), otherId);

        send(get("/members/{id}/loans", memberId), librarianToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        send(get("/members/999999/loans"), librarianToken()).andExpect(status().isNotFound());
    }
}