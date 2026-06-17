-- 시드 데이터: 테스트용 배당 정보
-- Spring Boot가 JPA 테이블 생성 후 자동 실행 (spring.jpa.defer-datasource-initialization=true 필요)

INSERT INTO dividend_info (id, stock_code, dividend_per_share, ex_dividend_date, record_date, payment_date, dividend_yield, year)
VALUES
  ('a1b2c3d4-0001-0001-0001-000000000001', '005930', 361, '2026-06-25', '2026-06-27', '2026-07-15', 0.0182, 2026),
  ('a1b2c3d4-0001-0001-0001-000000000002', '000660', 1200, '2026-06-25', '2026-06-27', '2026-07-20', 0.0095, 2026),
  ('a1b2c3d4-0001-0001-0001-000000000003', '035720', 500, '2026-09-25', '2026-09-27', '2026-10-15', 0.0120, 2026)
ON CONFLICT DO NOTHING;
