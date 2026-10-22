CREATE TABLE books (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    isbn             VARCHAR(20)  NOT NULL,
    author           VARCHAR(150) NOT NULL,
    published_year   INT          NOT NULL,
    total_copies     INT          NOT NULL,
    available_copies INT          NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_books_isbn UNIQUE (isbn),
    CONSTRAINT ck_books_total CHECK (total_copies >= 1),
    CONSTRAINT ck_books_available CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

CREATE INDEX idx_books_title ON books (lower(title));
CREATE INDEX idx_books_author ON books (lower(author));