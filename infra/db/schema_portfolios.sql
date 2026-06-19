-- Reference schema for portfolios table (full target state)
-- This is NOT a migration file - it documents the final desired schema

CREATE TABLE IF NOT EXISTS portfolios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    stock_code VARCHAR(10) NOT NULL,
    stock_name VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    ex_dividend_date DATE,
    dividend_per_share DECIMAL(15, 2) NOT NULL DEFAULT 0 CHECK (dividend_per_share >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, stock_code)
);

CREATE INDEX IF NOT EXISTS idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX IF NOT EXISTS idx_portfolios_ex_dividend_date ON portfolios(ex_dividend_date);
