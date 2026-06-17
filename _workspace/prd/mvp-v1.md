# PRD: 배당금 캘린더 & 절세 최적화 봇 — MVP v1

## 1. 개요

### 목표
카카오톡 챗봇으로 보유 종목의 배당금 정보를 조회하고,
배당락일 알림 및 ISA 절세 한도를 관리할 수 있는 서비스의 MVP 구축.

### 범위 (MVP 핵심 3개 서비스 + 관리자 대시보드)
- webhook-gateway: 카카오 챗봇 스킬 서버
- dividend-engine: 배당금 계산 & 월별 캘린더
- notification: 카카오 알림톡 발송
- admin-dashboard: 관리자 대시보드 (React)

### 범위 외 (Phase 2 이후)
- user-account 서비스 (MVP에서는 단순 인메모리/DB 직접 관리)
- market-data 서비스 (MVP에서는 dividend-engine 내부에서 직접 호출)
- scheduler 서비스 (MVP에서는 수동 트리거 또는 단순 cron)
- 해외 주식 지원
- Kafka 기반 이벤트 아키텍처

---

## 2. 기능 요구사항 (FR)

### FR-001: 포트폴리오 등록/조회
- 사용자가 카카오 챗봇에서 "삼성전자 100주 추가" 입력 시 포트폴리오에 저장
- "포트폴리오 보여줘" 입력 시 보유 종목 리스트 + 평가액 응답
- 저장소: PostgreSQL (user_portfolios 테이블)

### FR-002: 월별 배당 캘린더 조회
- "이번 달 배당금" 입력 시 보유 종목 기준 월별 예상 배당금 응답
- 세전/세후 금액 모두 표시
- 계좌 유형별 세율 적용 (일반 15.4%, ISA 비과세/9.9%)

### FR-003: 배당락일 알림
- 보유 종목의 배당락일 D-3, D-1에 카카오 알림톡 발송
- 알림 내용: 종목명, 배당락일, 보유수량, 예상 배당금

### FR-004: ISA 비과세 한도 조회
- "ISA 한도" 입력 시 현재 누적 배당소득 / 비과세 한도 잔여액 표시
- 한도 초과 임박 시 경고 메시지

### FR-005: 관리자 대시보드
- 전체 사용자 수, 포트폴리오 현황 조회
- 알림 발송 이력 조회
- 배당 데이터 수집 현황 모니터링
- 시스템 헬스 상태 표시

---

## 3. 비기능 요구사항 (NFR)

| 항목 | 기준 |
|------|------|
| 응답 시간 | 카카오 스킬 응답 < 3초 (5초 타임아웃 여유) |
| 가용성 | 99% (MVP 단계) |
| 보안 | API Key 환경변수 관리, DB 쿼리 파라미터화 |
| 데이터 | 금융 데이터 암호화 저장 불필요 (MVP), 로그 마스킹 필수 |

---

## 4. API 명세

### 4.1 Webhook Gateway (카카오 스킬 서버)

#### POST /api/kakao/skill
카카오 i 오픈빌더 스킬 요청 수신

Request:
```json
{
  "intent": {"name": "dividend.monthly"},
  "userRequest": {"utterance": "이번 달 배당금", "user": {"id": "kakao_user_123"}},
  "action": {"params": {}}
}
```

Response:
```json
{
  "version": "2.0",
  "template": {
    "outputs": [{"simpleText": {"text": "6월 예상 배당금: 152,300원 (세후)"}}]
  }
}
```

### 4.2 Dividend Engine (내부 API)

#### GET /api/dividends/monthly?user_id={id}&month={YYYY-MM}
월별 배당금 계산 결과 반환

#### GET /api/dividends/ex-dates?user_id={id}&days_ahead={n}
N일 내 배당락일 종목 조회

#### POST /api/portfolios
포트폴리오 종목 추가

#### GET /api/portfolios?user_id={id}
포트폴리오 조회

### 4.3 Notification Service (내부 API)

#### POST /api/notifications/send
알림톡 발송 요청

### 4.4 Admin Dashboard (프론트엔드)

#### GET /api/admin/stats
대시보드 통계 (사용자 수, 포트폴리오 수, 알림 발송 수)

#### GET /api/admin/notifications?page={n}
알림 발송 이력

#### GET /api/admin/health
시스템 상태

---

## 5. 데이터 모델

### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| kakao_user_id | VARCHAR(100) | 카카오 사용자 식별자 |
| account_type | ENUM | GENERAL, ISA_GENERAL, ISA_SPECIAL, IRP |
| created_at | TIMESTAMP | 가입일 |

### portfolios
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| user_id | UUID | FK → users |
| stock_code | VARCHAR(10) | 종목코드 (예: 005930) |
| stock_name | VARCHAR(50) | 종목명 |
| quantity | INTEGER | 보유 수량 |
| created_at | TIMESTAMP | 등록일 |

### dividend_info
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| stock_code | VARCHAR(10) | 종목코드 |
| dividend_per_share | DECIMAL(15,2) | 주당 배당금 |
| ex_dividend_date | DATE | 배당락일 |
| record_date | DATE | 배당기준일 |
| payment_date | DATE | 지급예정일 |
| dividend_yield | DECIMAL(5,4) | 배당수익률 |
| year | INTEGER | 배당 연도 |

### notification_logs
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| user_id | UUID | FK → users |
| type | VARCHAR(50) | 알림 유형 (EX_DATE_D3, EX_DATE_D1 등) |
| message | TEXT | 발송 메시지 내용 |
| status | ENUM | SENT, FAILED, PENDING |
| sent_at | TIMESTAMP | 발송 시각 |

---

## 6. 의존성 & 제약사항

| 항목 | 상세 |
|------|------|
| 카카오 비즈니스 채널 | 챗봇 오픈빌더 설정 필요 (수동) |
| 카카오 알림톡 | 템플릿 사전 승인 필요 (1~3일) |
| 한투 API | App Key 발급 필요, 초당 20건 제한 |
| PostgreSQL | AWS RDS 또는 로컬 Docker |
| MVP 한정 | 배당 데이터는 초기 시드 데이터 + 한투 API 직접 호출 |

---

## 7. 태스크 분할표

| ID | 태스크 | 서비스 | 복잡도 | 선행 | 담당 |
|----|--------|--------|--------|------|------|
| T-001 | 프로젝트 초기 세팅 (Go mod, Docker, DB 스키마) | infra | S | - | Backend Dev |
| T-002 | dividend-engine: 배당금 계산 로직 | dividend-engine | M | T-001 | Backend Dev 1 |
| T-003 | dividend-engine: 포트폴리오 CRUD API | dividend-engine | S | T-001 | Backend Dev 1 |
| T-004 | dividend-engine: 배당락일 조회 API | dividend-engine | S | T-002 | Backend Dev 1 |
| T-005 | webhook-gateway: 카카오 스킬 서버 기본 구조 | webhook-gateway | M | T-001 | Backend Dev 2 |
| T-006 | webhook-gateway: Intent 라우팅 & 응답 포맷팅 | webhook-gateway | M | T-005, T-002 | Backend Dev 2 |
| T-007 | notification: 알림톡 발송 서비스 | notification | M | T-001 | Backend Dev 3 |
| T-008 | notification: 배당락일 알림 트리거 | notification | S | T-004, T-007 | Backend Dev 3 |
| T-009 | admin-dashboard: React 프로젝트 세팅 + 기본 레이아웃 | admin | S | - | Frontend Dev |
| T-010 | admin-dashboard: 통계/이력/상태 페이지 구현 | admin | M | T-009, T-003 | Frontend Dev |
| T-011 | CI/CD: GitHub Actions 워크플로우 | infra | S | T-001 | Backend Dev |
| T-012 | Docker Compose 통합 환경 | infra | S | 전체 | Backend Dev |

### 병렬 실행 그룹
- **그룹 A** (T-001 완료 후 병렬): T-002, T-003, T-005, T-007
- **그룹 B** (그룹 A 일부 완료 후): T-004, T-006, T-008, T-010
- **독립**: T-009 (프론트는 API 목업으로 선행 개발 가능)
