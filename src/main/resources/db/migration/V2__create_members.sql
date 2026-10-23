CREATE TABLE members (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    phone      VARCHAR(20),
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_members_email UNIQUE (email),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);