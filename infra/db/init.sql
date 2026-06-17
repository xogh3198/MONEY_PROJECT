-- 배당금 봇 초기 스키마
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 시드 데이터: 배당 정보 (테스트용)
INSERT INTO dividend_info (id, stock_code, dividend_per_share, ex_dividend_date, record_date, payment_date, dividend_yield, year)
VALUES
  (uuid_generate_v4(), '005930', 361, '2026-06-25', '2026-06-27', '2026-07-15', 0.0182, 2026),
  (uuid_generate_v4(), '000660', 1200, '2026-06-25', '2026-06-27', '2026-07-20', 0.0095, 2026),
  (uuid_generate_v4(), '035720', 500, '2026-09-25', '2026-09-27', '2026-10-15', 0.0120, 2026)
ON CONFLICT DO NOTHING;
