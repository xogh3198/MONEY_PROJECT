-- Migration: Add dividend fields to portfolios table and enforce unique constraint
-- Applies to: PostgreSQL 16
-- Date: 2025-01-15

-- Add new columns if they don't exist
ALTER TABLE portfolios
    ADD COLUMN IF NOT EXISTS ex_dividend_date DATE,
    ADD COLUMN IF NOT EXISTS dividend_per_share DECIMAL(15, 2) NOT NULL DEFAULT 0;

-- Add CHECK constraint for dividend_per_share >= 0
ALTER TABLE portfolios
    ADD CONSTRAINT chk_dividend_per_share_non_negative CHECK (dividend_per_share >= 0);

-- Add CHECK constraint for quantity >= 1
ALTER TABLE portfolios
    ADD CONSTRAINT chk_quantity_positive CHECK (quantity >= 1);

-- Add UNIQUE constraint on (user_id, stock_code)
ALTER TABLE portfolios
    ADD CONSTRAINT uq_portfolios_user_stock UNIQUE (user_id, stock_code);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX IF NOT EXISTS idx_portfolios_ex_dividend_date ON portfolios(ex_dividend_date);
