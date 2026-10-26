# Bibliotech

![CI](https://github.com/<usuario>/bibliotech/actions/workflows/ci.yml/badge.svg)

API REST monolítica para gestão de biblioteca: acervo, membros e empréstimos, com regras de negócio reais (limites, prazos, renovação e multa por atraso), autenticação JWT e controle de acesso por perfil.

## Stack

Java 21 · Spring Boot 3 · Spring Data JPA · PostgreSQL 16 · Flyway · Spring Security (OAuth2 Resource Server / JWT) · springdoc-openapi · JUnit 5 · Mockito · Testcontainers · Docker

## Como rodar

**Só o banco, app pela IDE (perfil `dev`, com dados de exemplo):**

```bash
docker compose up -d db
./mvnw spring-boot:run
```

**Stack completa em containers (perfil `prod`):**

```bash
cp .env.example .env     # defina DB_PASSWORD, JWT_SECRET e ADMIN_PASSWORD
docker compose up --build
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Usuário admin de dev: `admin@bibliotech.local` / `admin12345` (somente no perfil `dev`)

## Endpoints

| Método | Rota | Acesso |
|---|---|---|
| POST | `/auth/login` | público |
| GET / POST / PUT | `/books`, `/books/{id}` | autenticado |
| DELETE | `/books/{id}` | ADMIN |
| GET / POST / PUT | `/members`, `/members/{id}` | autenticado |
| GET | `/members/{id}/loans` | autenticado |
| POST | `/loans` · `/loans/{id}/return` · `/loans/{id}/renew` | autenticado |
| GET | `/loans?status=&memberId=` · `/loans/overdue` | autenticado |
| POST | `/users` | ADMIN |

Listagens são paginadas (`page`, `size` até 100, `sort`). Erros seguem o formato RFC 7807 (`ProblemDetail`).

## Regras de negócio

1. Só há empréstimo se existirem exemplares disponíveis.
2. Máximo de 3 empréstimos ativos por membro; não é possível ter dois do mesmo livro.
3. Membro suspenso ou com empréstimo em atraso não pega novo livro.
4. Prazo de 14 dias; uma única renovação de +7 dias, apenas se não estiver em atraso.
5. Multa de R$ 0,50 por dia de atraso.
6. Livro com empréstimo ativo não pode ser excluído; o total de exemplares não pode ficar abaixo do que está emprestado.

## Decisões de arquitetura

- **Atraso e multa são derivados, não armazenados.** O banco guarda só `ACTIVE`/`RETURNED`; "atrasado" é `ACTIVE` com vencimento passado, e a multa é calculada a partir das datas. Elimina um estado que ficaria obsoleto sem job agendado e impede multa dessincronizada.
- **Lock pessimista no livro** durante empréstimo e devolução, para que duas requisições simultâneas não emprestem o último exemplar. Há teste de concorrência (8 threads, 1 exemplar, exatamente 1 sucesso). `@Version` permanece como segunda barreira.
- **JWT stateless (HS256)** validado pelo OAuth2 Resource Server do Spring Security, em vez de filtro caseiro. Respostas 401/403 no mesmo formato `ProblemDetail` do resto da API. Login responde igual para e-mail inexistente e senha errada.
- **Migrations Flyway** com `ddl-auto=validate`; o banco impõe as invariantes (`CHECK`, `UNIQUE`, FKs) além da validação na aplicação.
- **Status HTTP com semântica clara:** 409 para conflito de dado/estado (ISBN duplicado), 422 para regra de negócio violada, 400 para entrada inválida.
- **`Clock` injetável**, o que torna prazos e multas testáveis de forma determinística.
- **Configuração por perfil:** em `prod` não há valores padrão para segredos; a aplicação não sobe sem `DB_URL`, `JWT_SECRET` etc.
- **Cache (Caffeine) apenas em `GET /books/{id}`**, com invalidação explícita após commit e TTL de 30 s. Consistente somente com instância única; com réplicas, o caminho é Redis.

## Testes

```bash
./mvnw test      # unitários (regras de negócio, sem Spring)
./mvnw verify    # + integração com PostgreSQL real via Testcontainers (requer Docker) e relatório JaCoCo
```

Os testes de integração cobrem segurança (401/403, token expirado/adulterado/de outro emissor), regras de negócio ponta a ponta, paginação, tratamento de erros e concorrência.

## Melhorias futuras

- Rate limit no `/auth/login`
- Refresh token e revogação
- Exclusão lógica de livros com histórico de empréstimos
- Notificação de vencimento (o único caso em que um job agendado faz sentido)
- Cache distribuído se houver mais de uma instância