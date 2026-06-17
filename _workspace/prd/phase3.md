# PRD Phase 3: 한투 API 연동 + Scheduler + Swagger

## 목표
실제 한국투자증권 OpenAPI를 연동하여 배당 데이터를 자동 수집하고,
Scheduler로 배당락일 알림을 자동 트리거하며, API 문서를 자동 생성한다.

## 태스크

| ID | 태스크 | 서비스 | 복잡도 |
|----|--------|--------|--------|
| P3-001 | 한투 OpenAPI 클라이언트 (Token 관리, Rate Limit) | dividend-engine | M |
| P3-002 | 배당 데이터 수집 스케줄러 (@Scheduled, 매일 02:00) | dividend-engine | S |
| P3-003 | 배당락일 알림 트리거 (D-3, D-1 자동 발송) | dividend-engine | M |
| P3-004 | SpringDoc/Swagger API 문서 자동 생성 | 전 서비스 | S |
| P3-005 | 개발 현황 대시보드 Phase 3 태스크 반영 | admin-dashboard | S |
