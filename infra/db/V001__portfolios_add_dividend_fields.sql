-- Migration: Add dividend fields to portfolios table and enforce unique constraint
-- Applies to: PostgreSQL 16
-- Date: 2025-01-15

-- Add new columns if they don't exist
ALTER TABLE portfolios
    ADD COLUMN IF NOT EXISTS ex_dividend_date DATE,
    ADD COLUMN IF NOT EXISTS dividend_per_share DECIMAL(15, 2) NOT NULL DEFAULT 0;

-- Add constraints only when they do not already exist so redeployments are safe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_dividend_per_share_non_negative'
          AND conrelid = 'portfolios'::regclass
    ) THEN
        ALTER TABLE portfolios
            ADD CONSTRAINT chk_dividend_per_share_non_negative
            CHECK (dividend_per_share >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_quantity_positive'
          AND conrelid = 'portfolios'::regclass
    ) THEN
        ALTER TABLE portfolios
            ADD CONSTRAINT chk_quantity_positive CHECK (quantity >= 1);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_portfolios_user_stock'
          AND conrelid = 'portfolios'::regclass
    ) THEN
        ALTER TABLE portfolios
            ADD CONSTRAINT uq_portfolios_user_stock UNIQUE (user_id, stock_code);
    END IF;
END
$$;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX IF NOT EXISTS idx_portfolios_ex_dividend_date ON portfolios(ex_dividend_date);
