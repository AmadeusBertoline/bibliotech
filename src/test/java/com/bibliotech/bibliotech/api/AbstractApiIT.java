package com.bibliotech.bibliotech.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.bibliotech.bibliotech.domain.AppUser;
import com.bibliotech.bibliotech.domain.MemberStatus;
import com.bibliotech.bibliotech.domain.Role;
import com.bibliotech.bibliotech.respository.UserRepository;
import com.jayway.jsonpath.JsonPath;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiIT {

    protected static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final AtomicInteger SEQ = new AtomicInteger();

    // Container singleton: sobe uma vez e é reaproveitado por todas as classes de
    // IT
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static volatile String librarianToken;

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JsonMapper mapper;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CacheManager cacheManager;

    /**
     * Usuários ficam (o admin é criado uma vez no startup); o resto é limpo a cada
     * teste.
     */
    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE TABLE loans, books, members RESTART IDENTITY CASCADE");
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }
    
    // ---------- autenticação ----------

    protected String adminToken() throws Exception {
        return login("admin@test.local", "admin12345");
    }

    protected String librarianToken() throws Exception {
        if (librarianToken == null) {
            String email = "librarian@test.local";
            if (!userRepository.existsByEmail(email)) {
                AppUser user = new AppUser();
                user.setEmail(email);
                user.setPasswordHash(passwordEncoder.encode("librarian123"));
                user.setRole(Role.LIBRARIAN);
                userRepository.save(user);
            }
            librarianToken = login(email, "librarian123");
        }
        return librarianToken;
    }

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(jsonRequest(HttpMethod.POST, "/auth/login",
                Map.of("email", email, "password", password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    // ---------- requisições ----------

    protected ResultActions send(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mvc.perform(request);
    }

    protected MockHttpServletRequestBuilder jsonRequest(HttpMethod method, String url, Object body) {
        return request(method, url).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected MockHttpServletRequestBuilder loanRequest(long bookId, long memberId) {
        return jsonRequest(HttpMethod.POST, "/loans", Map.of("bookId", bookId, "memberId", memberId));
    }

    protected String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- fixtures (criadas pela própria API) ----------

    protected long createBook(int copies) throws Exception {
        return createBook("Livro " + SEQ.incrementAndGet(), copies);
    }

    protected long createBook(String title, int copies) throws Exception {
        Map<String, Object> body = Map.of(
                "title", title,
                "isbn", "978-%010d".formatted(SEQ.incrementAndGet()),
                "author", "Autor Teste",
                "publishedYear", 2020,
                "totalCopies", copies);
        return idOf(send(jsonRequest(HttpMethod.POST, "/books", body), librarianToken())
                .andExpect(status().isCreated()));
    }

    protected long createMember(MemberStatus status) throws Exception {
        int n = SEQ.incrementAndGet();
        Map<String, Object> body = Map.of(
                "name", "Membro " + n,
                "email", "membro" + n + "@test.local",
                "status", status.name());
        return idOf(send(jsonRequest(HttpMethod.POST, "/members", body), librarianToken())
                .andExpect(status().isCreated()));
    }

    protected long openLoan(long bookId, long memberId) throws Exception {
        return idOf(send(loanRequest(bookId, memberId), librarianToken()).andExpect(status().isCreated()));
    }

    protected int availableCopies(long bookId) throws Exception {
        String body = send(get("/books/{id}", bookId), librarianToken())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer available = JsonPath.read(body, "$.availableCopies");
        return available;
    }

    protected LocalDate today() {
        return LocalDate.now(ZONE);
    }

    private long idOf(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}