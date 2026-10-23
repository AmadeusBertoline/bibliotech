CREATE TABLE loans (
    id           BIGSERIAL PRIMARY KEY,
    book_id      BIGINT      NOT NULL REFERENCES books (id),
    member_id    BIGINT      NOT NULL REFERENCES members (id),
    loan_date    DATE        NOT NULL,
    due_date     DATE        NOT NULL,
    return_date  DATE,
    renewals     INT         NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_loans_status CHECK (status IN ('ACTIVE', 'RETURNED')),
    CONSTRAINT ck_loans_renewals CHECK (renewals >= 0 AND renewals <= 1),
    CONSTRAINT ck_loans_return CHECK (
        (status = 'RETURNED' AND return_date IS NOT NULL) OR
        (status = 'ACTIVE' AND return_date IS NULL))
);

CREATE INDEX idx_loans_status_due ON loans (status, due_date);
CREATE INDEX idx_loans_member_status ON loans (member_id, status);
CREATE INDEX idx_loans_book_status ON loans (book_id, status);