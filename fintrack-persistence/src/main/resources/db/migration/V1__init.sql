
CREATE TABLE USERS (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ACCOUNTS (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES USERS(id),
    name VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,          -- optimistic locking (Version)
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE CATEGORIES (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES USERS(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE TRANSACTIONS (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES ACCOUNTS(id),
    category_id BIGINT NOT NULL REFERENCES CATEGORIES(id),
    amount DECIMAL(19, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    transaction_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE BUDGETS (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES USERS(id),
    category_id BIGINT NOT NULL REFERENCES CATEGORIES(id),
    amount DECIMAL(19, 2) NOT NULL,
    month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INT NOT NULL CHECK (year >= 2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_budgets_user_category_period UNIQUE (user_id, category_id, month, year)
);



-- Index for user and currency
CREATE INDEX idx_accounts_user_currency ON ACCOUNTS(user_id, currency);

-- Index for user, category and period
CREATE INDEX idx_budgets_user_category_period ON BUDGETS(user_id, category_id, year, month);
