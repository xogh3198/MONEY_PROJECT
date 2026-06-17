# PRD Phase 2: API 연동 & 서비스 통합

## 목표
MVP v1의 하드코딩 응답을 제거하고, 서비스 간 실제 API 통신을 구현한다.
notification 서비스를 추가하여 카카오 알림톡 발송 파이프라인을 완성한다.

## 태스크

| ID | 태스크 | 서비스 | 복잡도 |
|----|--------|--------|--------|
| P2-001 | webhook-gateway → dividend-engine WebClient 실제 연동 | webhook-gateway | M |
| P2-002 | dividend-engine: Admin API (/api/admin/stats) 구현 | dividend-engine | S |
| P2-003 | notification 서비스: Spring Boot 프로젝트 + 알림톡 발송 로직 | notification | M |
| P2-004 | GlobalExceptionHandler 추가 (전 서비스) | 공통 | S |
| P2-005 | admin-dashboard: 실제 API 연동 (하드코딩 제거) | admin-dashboard | S |
