package com.bibliotech.bibliotech.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthSecurityIT extends AbstractApiIT {

    @Autowired JwtEncoder jwtEncoder;

    // ---------- login ----------

    @Test
    void login_withValidCredentials_returnsBearerToken() throws Exception {
        mvc.perform(jsonRequest(HttpMethod.POST, "/auth/login",
                        Map.of("email", "admin@test.local", "password", "admin12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_wrongPasswordAndUnknownEmail_areIndistinguishable() throws Exception {
        mvc.perform(jsonRequest(HttpMethod.POST, "/auth/login",
                        Map.of("email", "admin@test.local", "password", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));

        mvc.perform(jsonRequest(HttpMethod.POST, "/auth/login",
                        Map.of("email", "ninguem@test.local", "password", "qualquer-coisa")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));
    }

    @Test
    void login_withInvalidPayload_returns400() throws Exception {
        mvc.perform(jsonRequest(HttpMethod.POST, "/auth/login", Map.of("email", "nao-e-email", "password", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    // ---------- autenticação ----------

    @Test
    void requestWithoutToken_returns401ProblemDetail() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void tamperedToken_returns401() throws Exception {
        send(get("/books"), adminToken() + "x").andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        String expired = forgeToken("bibliotech", Instant.now().minus(2, ChronoUnit.HOURS));

        send(get("/books"), expired).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFromAnotherIssuer_returns401() throws Exception {
        String foreign = forgeToken("outro-emissor", Instant.now().plus(1, ChronoUnit.HOURS));

        send(get("/books"), foreign).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token forjado com emissor e validade corretos é aceito")
    void forgedTokenWithCorrectIssuerAndExpiry_isAccepted() throws Exception {
        // controle: prova que os 401 acima vêm do defeito específico, não do helper de forja
        String valid = forgeToken("bibliotech", Instant.now().plus(1, ChronoUnit.HOURS));

        send(get("/books"), valid).andExpect(status().isOk());
    }

    @Test
    void health_isPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------- autorização ----------

    @Test
    void librarian_cannotDeleteBook_butAdminCan() throws Exception {
        long bookId = createBook(1);

        send(delete("/books/{id}", bookId), librarianToken())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));

        send(delete("/books/{id}", bookId), adminToken()).andExpect(status().isNoContent());
        send(get("/books/{id}", bookId), librarianToken()).andExpect(status().isNotFound());
    }

    @Test
    void librarian_cannotCreateUsers() throws Exception {
        send(jsonRequest(HttpMethod.POST, "/users",
                Map.of("email", "novo@test.local", "password", "senha12345", "role", "LIBRARIAN")),
                librarianToken())
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_createsUser_whoCanThenLogIn_andDuplicateIsRejected() throws Exception {
        Map<String, String> body = Map.of("email", "Novo.Usuario@Test.local", "password", "senha12345", "role", "LIBRARIAN");

        send(jsonRequest(HttpMethod.POST, "/users", body), adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo.usuario@test.local"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        login("novo.usuario@test.local", "senha12345");

        send(jsonRequest(HttpMethod.POST, "/users", body), adminToken()).andExpect(status().isConflict());
    }

    @Test
    void admin_cannotCreateUserWithShortPassword() throws Exception {
        send(jsonRequest(HttpMethod.POST, "/users",
                Map.of("email", "curta@test.local", "password", "123", "role", "LIBRARIAN")), adminToken())
                .andExpect(status().isBadRequest());
    }

    // ---------- helper ----------

    private String forgeToken(String issuer, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .subject("admin@test.local")
                .claim("roles", List.of("ADMIN"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}