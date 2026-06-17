---
inclusion: always
---

## 배당금 캘린더 & 절세 최적화 봇

### 프로젝트 개요
카카오톡 챗봇 기반 배당금 관리 서비스.
한국투자증권 OpenAPI로 배당 데이터를 수집하고,
ISA/IRP 계좌 절세 최적화 알림을 카카오 알림톡으로 발송한다.

### 기술 스택
- 백엔드: Java 17 + Spring Boot 3.3 (주력)
- 프론트엔드: React 18 + TypeScript + Vite (관리자 대시보드)
- DB: AWS RDS PostgreSQL 16
- ORM: Spring Data JPA + QueryDSL
- 메시지 큐: Apache Kafka (KRaft mode)
- 오브젝트 스토리지: MinIO (S3 호환)
- 배포: AWS EC2 + K3s + ArgoCD (GitOps)
- CI/CD: GitHub Actions
- 메신저: 카카오톡 (i 오픈빌더 챗봇 + 알림톡)
- 데이터 소스: 한국투자증권 OpenAPI (국내 → 해외 확장)
- 빌드: Gradle (Kotlin DSL)
- 테스트: JUnit 5 + Mockito + Testcontainers

### 서비스 구성 (MSA)
| 서비스 | 언어 | 역할 |
|--------|------|------|
| webhook-gateway | Go | 카카오 스킬 서버, 메시지 라우팅 |
| user-account | Go | 사용자, 포트폴리오, ISA/IRP 계좌 관리 |
| market-data | Python | 한투 API 배당 데이터 수집, 캐싱 |
| dividend-engine | Go | 월별 배당 계산, 전략 분석, ISA 한도 최적화 |
| notification | Go | 카카오 알림톡 발송, 발송 이력 관리 |
| scheduler | Go | 일배치 크론잡 (배당락일 체크, 알림 트리거) |

### 에이전트 트리거 규칙
- 기획/PRD/기능 설계 요청 → Planner 에이전트 역할로 수행
- 코드 구현/개발 요청 → Developer 에이전트 역할로 수행
- 코드 리뷰/품질 검증 요청 → Reviewer 에이전트 역할로 수행
- 단순 질문/설명 요청 → 직접 응답 (에이전트 불필요)

### 변경 이력
| 날짜 | 변경 내용 | 대상 | 사유 |
|------|----------|------|------|
| 2026-06-17 | 초기 하네스 구성 | 전체 | 프로젝트 시작 |
