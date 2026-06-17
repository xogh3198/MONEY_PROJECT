# 배당금 캘린더 & 절세 최적화 봇 — AI-Driven 개발 시스템 아키텍처

## 1. 전체 시스템 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Human (SRE/최종 의사결정자)                            │
│   - 비용 관리, 트래픽 모니터링, 최종 PR 승인, 배포 트리거                         │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ 기획 요청 / PR 승인·반려
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AI Orchestration Layer                                │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │ Planner  │→ │ Dev AI Pool  │→ │ Reviewer AI  │→ │ Deploy Pipeline    │  │
│  │ AI       │  │ (동적 생성)   │  │              │  │ (CI/CD)            │  │
│  └──────────┘  └──────────────┘  └──────────────┘  └────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    배당금 봇 서비스 (Production K3s Cluster)                   │
│  Ingress → Kafka → Microservices → PostgreSQL/MinIO → Messenger Bot         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. AI 역할 분담 체계 (Multi-Agent System)

### 2.1 Planner AI (기획자 AI)

**역할:** 인간이 던진 기획 요청을 받아 PRD를 생성하고,
기술적 실현 가능성을 검증한 뒤 개발 태스크를 분할합니다.

| 항목 | 상세 |
|------|------|
| 입력 | 인간의 기획 요청 (자연어) |
| 출력 | PRD, 기술 타당성 보고서, 태스크 분할표 |
| 도구 | Web Search, 기존 코드베이스 분석, API 문서 조회 |
| 판단 | 기능별 복잡도를 측정해 Developer AI 수를 동적 결정 |

**Planner AI의 워크플로우:**
```
1. 기획 요청 수신
2. 자료 조사 (경쟁사 분석, API 가용성 확인, 법률/규제 검토)
3. 기술 타당성 평가 (사용 가능 API 제한, 비용 산정, 성능 요구사항)
4. PRD 문서 생성
   - 기능 요구사항 (Functional Requirements)
   - 비기능 요구사항 (NFR: 성능, 보안, 가용성)
   - API 명세 초안
   - 데이터 모델 초안
5. 태스크 분할 & Developer AI 할당표 생성
   - 각 태스크별 예상 복잡도 (S/M/L)
   - 의존성 그래프 (어떤 서비스가 먼저 완성되어야 하는지)
   - 병렬 작업 가능 여부 판단
6. 인간에게 PRD 리뷰 요청 (선택적)
7. 승인 후 Developer AI Pool에 태스크 발행
```

### 2.2 Developer AI Pool (개발자 AI 그룹)

**역할:** Planner AI가 할당한 태스크를 받아 실제 코드를 작성하고 PR을 생성합니다.

| 유형 | 담당 영역 | 기술 스택 |
|------|-----------|-----------|
| Backend Dev AI #1~N | 마이크로서비스 개발 | Go / Spring Boot, gRPC, Kafka |
| Frontend Dev AI | 관리자 대시보드 (선택) | React, Next.js |
| Infra Dev AI | Helm Chart, K3s 매니페스트 | Terraform, Helm, ArgoCD |
| Data Dev AI | 배당 데이터 파이프라인 | Python, Apache Airflow |

**Developer AI의 워크플로우:**
```
1. Planner AI로부터 태스크 티켓 수신
   - 명확한 API 명세 (입력/출력 스키마)
   - 테스트 케이스 시나리오
   - 의존 서비스 인터페이스 정보
2. feature branch 생성 (feat/{service-name}/{task-id})
3. 코드 작성
   - 비즈니스 로직 구현
   - 단위 테스트 작성 (coverage 80%+ 목표)
   - 통합 테스트 작성
   - API 문서 자동 생성 (OpenAPI spec)
4. self-review (린트, 포맷, 보안 스캔)
5. PR 생성 → Reviewer AI에게 전달
```

### 2.3 Reviewer AI (코드 리뷰 AI)

**역할:** Developer AI가 올린 PR을 검증하고, 품질 리포트를 인간에게 전달합니다.

**리뷰 체크리스트:**
```
□ 코드 품질
  - 코드 스타일 & 린트 규칙 준수
  - 함수 복잡도 (Cyclomatic Complexity < 10)
  - 중복 코드 탐지
  - 에러 핸들링 완전성

□ 보안
  - SQL Injection / XSS 취약점
  - API Key 하드코딩 여부
  - 입력값 검증 누락

□ 성능
  - N+1 쿼리 패턴 탐지
  - 불필요한 메모리 할당
  - Kafka 메시지 처리 병목 가능성

□ 인프라 호환성
  - K3s 리소스 제한(limits/requests) 적정성
  - 환경변수 설정 누락
  - 헬스체크 엔드포인트 구현 여부

□ 테스트
  - 테스트 커버리지 충족
  - 엣지 케이스 커버 여부
  - Mock 사용의 적절성
```

**Reviewer AI 출력물:**
```
- PR 요약 (변경 파일 수, 추가/삭제 라인)
- 심각도별 이슈 분류 (Critical / Warning / Info)
- 승인 추천 여부 (Approve / Request Changes)
- 인간용 요약 메시지 (3줄 이내)
```

### 2.4 Harness Layer (하네스 엔지니어링)

**역할:** revfactory/harness의 설계 원칙을 적용하여 AI Agent 팀의 출력 품질을 
**모델 변경 없이** 시스템적으로 보장합니다. (+60% 품질 향상, 100% 승률 — A/B 테스트 검증)

| 하네스 구성요소 | 역할 | Kiro 구현 |
|---------------|------|-----------|
| 에이전트 정의 (누가) | 역할, 원칙, 입출력 프로토콜 | `.kiro/agents/{role}.md` |
| 스킬 (어떻게) | 작업 절차, 도메인 규칙 | `.kiro/steering/*.md` (Progressive Disclosure) |
| 오케스트레이터 | 팀 조율, 데이터 흐름 | `.kiro/steering/orchestrator.md` |
| 가드레일 | 금지 사항 구조적 강제 | `.kiro/steering/financial-rules.md` |
| 검증 루프 | 출력물 자동 검증 | `.kiro/hooks/*.json` |
| 메모리 | 세션 간 학습 축적 | `ai-agents/memory/` |

**아키텍처 패턴:** Supervisor(Planner) + Producer-Reviewer(Dev→Reviewer) 하이브리드
**진화 원칙:** 실패 발생 → 유형 분류 → 해당 steering/hook 수정 → 동일 실패 구조적 방지
상세: `docs/architecture/HARNESS_ENGINEERING.md` 참조

### 2.5 역할 간 상호작용 흐름

```
Human ──(기획)──→ Planner AI
                      │
                      ├──(PRD + 태스크)──→ Dev AI #1 ──(PR)──→ Reviewer AI
                      ├──(PRD + 태스크)──→ Dev AI #2 ──(PR)──→ Reviewer AI
                      └──(PRD + 태스크)──→ Dev AI #3 ──(PR)──→ Reviewer AI
                                                                    │
                                                         ┌──────────┴──────────┐
                                                         │ 리뷰 리포트          │
                                                         └──────────┬──────────┘
                                                                    ▼
                                                               Human (최종 확인)
                                                                    │
                                                    ┌───────────────┼───────────────┐
                                                    ▼               ▼               ▼
                                                 승인 →          반려 →          보류 →
                                                 Merge &        Planner에게      추가 논의
                                                 Deploy         피드백 전달
```

---

## 3. CI/CD 파이프라인 아키텍처

### 3.1 파이프라인 전체 흐름

```
[PR 생성] → [Automated Tests] → [Reviewer AI 분석] → [Human 승인] → [Merge] → [Build] → [Deploy]
     │              │                    │                  │              │          │
     ▼              ▼                    ▼                  ▼              ▼          ▼
  GitHub        GitHub Actions      Custom AI Agent     Slack/Telegram   ArgoCD    K3s Cluster
  Branch        (lint, test,        (코드 리뷰 +        알림 전송        GitOps    Rolling
  Protection    security scan)      PR 코멘트)                           Sync      Update
```

### 3.2 단계별 상세

| 단계 | 도구 | 트리거 | 실패 시 |
|------|------|--------|---------|
| 1. PR Open | GitHub Actions | PR 이벤트 | - |
| 2. Lint & Format | ESLint, golangci-lint | 자동 | PR 블록 |
| 3. Unit Test | Jest, Go test | 자동 | PR 블록 |
| 4. Integration Test | Docker Compose 기반 | 자동 | PR 블록 |
| 5. Security Scan | Trivy, Snyk | 자동 | Critical이면 블록 |
| 6. Reviewer AI | Custom GitHub Action | CI 통과 후 | Request Changes 코멘트 |
| 7. Human Approval | GitHub PR Review | 알림 수신 후 | 반려 → Planner에 피드백 |
| 8. Merge to main | GitHub | 승인 후 자동 | - |
| 9. Container Build | GitHub Actions + Kaniko | Merge 이벤트 | 롤백 |
| 10. Deploy | ArgoCD (GitOps) | Image 태그 변경 감지 | 자동 롤백 |

### 3.3 배포 전략

```yaml
# ArgoCD Application 예시
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: dividend-bot
spec:
  destination:
    server: https://kubernetes.default.svc
    namespace: dividend-prod
  source:
    repoURL: https://github.com/your-org/dividend-bot-infra
    path: k3s/overlays/production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

**배포 모드:**
- **Blue-Green**: 신규 버전을 별도 Pod으로 띄운 뒤 트래픽 전환
- **Canary**: 트래픽 10% → 50% → 100% 점진 전환
- **자동 롤백**: Health Check 실패 시 이전 버전으로 즉시 복구

---

## 4. 배당금 봇 서비스 아키텍처 (Production)

### 4.1 마이크로서비스 구성도

```
┌─────────────────────────────────────────────────────────────────┐
│                      K3s Cluster (Production)                    │
│                                                                  │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │  Ingress    │    │   Kafka     │    │    PostgreSQL        │  │
│  │  (Traefik)  │    │   Cluster   │    │    (HA - Patroni)   │  │
│  └──────┬──────┘    └──────┬──────┘    └──────────┬──────────┘  │
│         │                  │                      │              │
│  ┌──────▼──────┐    ┌─────▼──────┐    ┌─────────▼───────────┐  │
│  │  Webhook    │    │  Event     │    │   MinIO              │  │
│  │  Gateway    │───▶│  Router    │    │   (Object Storage)   │  │
│  └─────────────┘    └─────┬──────┘    └──────────────────────┘  │
│                           │                                      │
│         ┌─────────────────┼─────────────────┐                   │
│         ▼                 ▼                  ▼                   │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐        │
│  │  User &     │  │  Market     │  │  Dividend &      │        │
│  │  Account    │  │  Data       │  │  Strategy        │        │
│  │  Service    │  │  Aggregator │  │  Engine          │        │
│  └─────────────┘  └─────────────┘  └──────────────────┘        │
│         │                 │                  │                   │
│         └─────────────────┼──────────────────┘                   │
│                           ▼                                      │
│                   ┌─────────────┐                                │
│                   │ Notification│                                │
│                   │ Service     │                                │
│                   └──────┬──────┘                                │
│                          │                                       │
└──────────────────────────┼───────────────────────────────────────┘
                           ▼
                 ┌───────────────────┐
                 │ Telegram / Kakao  │
                 │ Messenger Bot     │
                 └───────────────────┘
```

### 4.2 서비스별 상세 스펙

| 서비스 | 언어 | 주요 기능 | 저장소 |
|--------|------|-----------|--------|
| Webhook Gateway | Go | 메신저 웹훅 수신, JWT 인증, 메시지 큐 전달 | - |
| User & Account Service | Go | 사용자/포트폴리오/ISA·IRP 계좌 CRUD | PostgreSQL |
| Market Data Aggregator | Python | 배당 데이터 수집, 주가 API 연동, 캐싱 | MinIO |
| Dividend & Strategy Engine | Go | 월별 배당 계산, 배당락일 전략, ISA 한도 최적화 | PostgreSQL |
| Notification Service | Go | 알림 포맷팅, 메신저 API 전송, 발송 이력 관리 | PostgreSQL |
| Scheduler (CronJob) | Go | 일배치: 배당락일 체크, 알림 트리거 이벤트 발행 | - |

### 4.3 데이터 흐름 타임라인

```
[매일 02:00 KST] Market Data Aggregator
  → 한국투자증권 OpenAPI 호출 (Token 재발급 → 배당정보 수집)
  → 배당락일, 배당금액, 배당수익률 수집
  → MinIO에 JSON 캐시 저장
  → Kafka topic: market.dividend.updated 발행

[매일 08:00 KST] Scheduler CronJob
  → Kafka topic: schedule.daily.check 발행

[08:00~08:05] Dividend & Strategy Engine
  → market.dividend.updated + schedule.daily.check 컨슘
  → 오늘 배당락일 종목 보유자 필터링
  → ISA 비과세 한도 잔여액 계산
  → 배당 캡쳐 전략 분석 결과 생성
  → Kafka topic: notification.send 발행

[08:05~08:10] Notification Service
  → notification.send 컨슘
  → 카카오 알림톡 메시지 템플릿 렌더링
  → 카카오 비즈메시지 API 전송
  → 발송 결과 DB 기록
```

---

## 5. 인간(SRE)의 관리 영역

### 5.1 모니터링 & 옵저버빌리티 스택

```
┌────────────────────────────────────────────┐
│         Observability Stack                 │
│                                            │
│  ┌──────────┐  ┌──────────┐  ┌─────────┐  │
│  │Prometheus│  │  Grafana  │  │  Loki   │  │
│  │(메트릭)  │  │(대시보드) │  │ (로그)  │  │
│  └──────────┘  └──────────┘  └─────────┘  │
│                                            │
│  ┌──────────┐  ┌──────────────────────┐    │
│  │  Jaeger  │  │  AlertManager        │    │
│  │(트레이싱)│  │  → Telegram 알림     │    │
│  └──────────┘  └──────────────────────┘    │
└────────────────────────────────────────────┘
```

### 5.2 SRE 대시보드 핵심 지표

| 카테고리 | 지표 | 임계값 |
|----------|------|--------|
| 비용 | 월간 외부 API 호출 비용 | > $50 경고 |
| 비용 | 클러스터 컴퓨팅 비용 (EC2) | > $100 경고 |
| 비용 | AI Agent API 토큰 비용 | Kiro 구독 내 포함 |
| 트래픽 | 동시 활성 사용자 수 | > 1,000 스케일업 |
| 트래픽 | Kafka consumer lag | > 10,000 경고 |
| 가용성 | 서비스 Health Check | 3연속 실패 → PagerDuty |
| 가용성 | 알림 전송 성공률 | < 99% 경고 |
| 성능 | Webhook 응답 시간 P95 | > 200ms 경고 |
| 성능 | 배당 계산 배치 소요시간 | > 5분 경고 |

### 5.3 비용 최적화 전략

```
1. 컴퓨팅 비용
   - AWS Spot Instance: Market Data Aggregator (새벽 배치, 중단 허용)
   - On-Demand: Webhook Gateway (가용성 최우선)
   - Reserved: PostgreSQL, Kafka (항상 가동)

2. API 호출 비용 절감
   - MinIO 캐시로 동일 데이터 재호출 방지
   - Rate Limiting: 분당 60회 제한 준수 (Token Bucket)
   - 배당 정보는 하루 1회 수집이면 충분 (실시간 불필요)

3. AI Agent 비용 관리
   - Kiro IDE 내 Claude Opus 4.6 활용 (별도 API 비용 없음)
   - 캐싱: 동일 코드 패턴은 템플릿 재사용
   - Iteration 제한: Planner↔Dev 최대 3회 반복 후 인간 개입

4. 클러스터 오토스케일링
   - HPA: CPU 70% 초과 시 Pod 수평 확장
   - 야간(01:00~06:00) 최소 레플리카로 축소
   - VPA: 리소스 사용량 학습 후 requests/limits 자동 조정
```

---

## 6. 추가 고려사항 & 위험 요소

### 6.1 기술적 고려사항

| 항목 | 위험 | 대응 방안 |
|------|------|-----------|
| API Rate Limit | 한국투자증권 API 분당 호출 제한 | Token Bucket 패턴, 요청 큐잉 |
| 데이터 정합성 | 배당금 정보 정정 발생 가능 | 이벤트 소싱으로 변경 이력 추적 |
| 메신저 API 변경 | 카카오/텔레그램 API 버전업 | Adapter 패턴으로 메신저별 모듈 분리 |
| 장애 전파 | 하나의 서비스 장애 → 전체 영향 | Circuit Breaker (Resilience4j) |
| 시간대 처리 | 한국/미국 시장 영업일, 공휴일 차이 | Calendar Service + 공휴일 DB |
| Kafka 데이터 유실 | 컨슈머 크래시 시 메시지 소실 | acks=all, min.insync.replicas=2 |

### 6.2 법률/규제 고려사항

| 항목 | 내용 | 대응 |
|------|------|------|
| 투자조언 vs 정보제공 | 투자 "조언"은 금융투자업 등록 필요 | "정보 제공"으로 한정, 면책 문구 필수 |
| 개인정보보호법 | 보유 종목 = 민감 금융정보 | AES-256 암호화 저장, 동의 절차 |
| 전자금융거래법 | 직접 매매 기능 없으므로 해당 없음 | 향후 기능 확장 시 재검토 |
| 통신비밀보호법 | 메신저 대화 내용 저장 | 최소 수집 원칙, 보존 기간 고지 |

### 6.3 AI Agent 운영 리스크

| 리스크 | 설명 | 대응 |
|--------|------|------|
| Hallucination | 존재하지 않는 API 호출 코드 생성 | Reviewer AI가 실제 API 문서 대조 검증 |
| 무한 루프 | Planner ↔ Dev 사이 수정 반복 | 최대 3회 iteration 제한 → 인간 개입 |
| 비용 폭주 | AI API 호출(GPT-4 등) 비용 과다 | 일일 토큰 예산 상한 + 알림 |
| 보안 취약 코드 | AI 생성 코드에 취약점 포함 가능 | Trivy/Snyk CI 필수 통과 |
| 코드 스타일 불일치 | Agent마다 코딩 스타일 차이 | 공통 린트/포맷 규칙 강제 + 코드 템플릿 |
| 컨텍스트 유실 | 긴 대화에서 초기 요구사항 망각 | PRD 문서를 매 요청마다 컨텍스트로 주입 |

### 6.4 AI Agent 오케스트레이션 구현 방안

```
Option A: GitHub Actions + Custom Webhook (추천 - MVP)
  - Planner AI: GitHub Issue 생성 시 트리거
  - Dev AI: Issue에서 branch 자동 생성 → 코드 작성 → PR
  - Reviewer AI: PR 이벤트 트리거 → 코멘트 작성
  - 장점: GitHub 생태계 활용, 추가 인프라 불필요
  - 단점: 복잡한 워크플로우 표현 한계

Option B: LangGraph / CrewAI (확장 시)
  - 중앙 오케스트레이터가 각 Agent의 상태를 관리
  - DAG(방향 비순환 그래프)로 워크플로우 정의
  - 장점: 복잡한 분기/합류 로직 구현 가능
  - 단점: 별도 서버 운영 필요

Option C: Temporal (대규모 확장 시)
  - 워크플로우 엔진으로 Agent 간 통신 관리
  - 장점: 재시도, 타임아웃, 상태 복구 내장
  - 단점: 학습 곡선 높음, 인프라 복잡도 증가
```

---

## 7. MVP 단계 구분 (Phase Plan)

```
Phase 1 (MVP - 4주) ──────────────────────────────────────
├── 카카오톡 챗봇 기본 연동 (Kakao i 오픈빌더)
├── 한국투자증권 OpenAPI 연동 (국내 배당 데이터 수집)
├── 월별 배당 캘린더 조회 기능
├── 단일 서버 배포 (Docker Compose)
├── GitHub Actions CI 기본 구축
└── Planner AI 프롬프트 & PRD 템플릿 확정

Phase 2 (Core Features - 4주) ────────────────────────────
├── ISA/IRP 계좌 한도 추적 & 비과세 한도 알림
├── 배당락일 D-3, D-1 사전 알림 (카카오 알림톡)
├── 사용자 포트폴리오 CRUD (카카오 챗봇 대화형)
├── Reviewer AI 구축 (PR 자동 리뷰 GitHub Action)
└── AWS RDS PostgreSQL 마이그레이션

Phase 3 (Scale & Strategy - 4주) ─────────────────────────
├── 해외 주식 배당 데이터 확장 (한투 해외주식 API)
├── 배당 캡쳐 전략 분석 엔진
├── Kafka 기반 이벤트 아키텍처 전환
├── K3s 클러스터 마이그레이션 (EC2)
├── 모니터링 스택 구축 (Prometheus + Grafana)
└── AI Dev Pool 동적 생성 자동화

Phase 4 (Optimization - 지속) ────────────────────────────
├── AI 기반 포트폴리오 배당 최적화 제안
├── 세금 시뮬레이터 (일반 vs ISA vs IRP 비교)
├── 배당 재투자 시뮬레이션
├── 텔레그램 봇 연동 (글로벌 확장)
├── 다중 사용자 관리 대시보드
└── 성능 최적화 & 비용 절감 자동화
```

---

## 8. 기술 스택 요약

| 레이어 | 기술 | 선택 이유 |
|--------|------|-----------|
| 메신저 봇 | 카카오톡 챗봇 (알림톡) | 한국 사용자 접근성 최우선 |
| 메신저 확장 | Telegram Bot API | 해외 사용자 대응 (Phase 4) |
| API Gateway | Traefik (K3s 기본) | K3s 내장, TLS 자동, 설정 간편 |
| 메시지 큐 | Apache Kafka (KRaft) | ZooKeeper-less 경량, 이벤트 소싱 |
| 백엔드 (주력) | Go 1.22+ | 고성능 동시처리, 컨테이너 경량화 |
| 데이터 파이프라인 | Python 3.12 | pandas/requests 생태계 활용 |
| 데이터베이스 | PostgreSQL 16 | JSONB, 트랜잭션, 성숙한 생태계 |
| 오브젝트 스토리지 | MinIO | S3 호환, 셀프호스팅, 비용 절감 |
| 컨테이너 | K3s (K8s 경량) | 단일 노드 가능, 리소스 효율적 |
| CI/CD | GitHub Actions + ArgoCD | GitOps, 선언적 배포, 자동 롤백 |
| 모니터링 | Prometheus + Grafana + Loki | 오픈소스, K8s 네이티브 통합 |
| 트레이싱 | Jaeger | 분산 트레이싱, OpenTelemetry 호환 |
| IaC | Terraform + Helm | 인프라 재현성, 버전 관리 |
| AI Agent | Kiro (Claude Opus 4.6) | IDE 통합, 코드 품질 우수 |
| Agent 오케스트레이션 | GitHub Actions (MVP) → LangGraph (Scale) | 점진적 복잡도 증가 |

---

## 9. 리포지토리 구조 (Mono-repo)

```
dividend-bot/
├── .github/
│   └── workflows/
│       ├── ci-backend.yml          # Go 서비스 린트/테스트/빌드
│       ├── ci-data.yml             # Python 데이터 파이프라인 CI
│       ├── ci-security.yml         # Trivy/Snyk 보안 스캔
│       ├── ai-reviewer.yml         # Reviewer AI PR 코멘트
│       └── cd-deploy.yml           # ArgoCD sync 트리거
├── docs/
│   ├── architecture/               # 시스템 아키텍처 문서 (이 파일)
│   ├── prd/                        # Planner AI 생성 PRD 저장소
│   ├── api-specs/                  # OpenAPI 스펙 (서비스별)
│   └── runbooks/                   # 장애 대응 매뉴얼
├── services/
│   ├── webhook-gateway/            # Go - 메신저 웹훅 수신
│   │   ├── cmd/main.go
│   │   ├── internal/
│   │   ├── Dockerfile
│   │   └── go.mod
│   ├── user-account/               # Go - 사용자/계좌 관리
│   ├── market-data/                # Python - 시장 데이터 수집
│   │   ├── src/
│   │   ├── Dockerfile
│   │   └── pyproject.toml
│   ├── dividend-engine/            # Go - 배당 계산/전략
│   ├── notification/               # Go - 알림 전송
│   └── scheduler/                  # Go - 크론잡 스케줄러
├── infra/
│   ├── terraform/
│   │   ├── aws/                    # EC2, VPC, Security Groups
│   │   └── modules/
│   ├── helm/
│   │   ├── kafka/
│   │   ├── postgresql/
│   │   ├── minio/
│   │   └── monitoring/
│   └── k3s/
│       ├── base/                   # Kustomize base manifests
│       └── overlays/
│           ├── dev/
│           ├── staging/
│           └── production/
├── ai-agents/
│   ├── planner/
│   │   ├── system-prompt.md        # Planner AI 시스템 프롬프트
│   │   ├── prd-template.md         # PRD 생성 템플릿
│   │   └── task-split-rules.md     # 태스크 분할 규칙
│   ├── developer/
│   │   ├── system-prompt.md        # Dev AI 시스템 프롬프트
│   │   ├── code-style-guide.md     # 코딩 컨벤션
│   │   └── templates/              # 서비스 보일러플레이트
│   └── reviewer/
│       ├── system-prompt.md        # Reviewer AI 시스템 프롬프트
│       ├── checklist.md            # 리뷰 체크리스트
│       └── severity-rules.md       # 이슈 심각도 판단 기준
├── shared/
│   ├── proto/                      # gRPC Protobuf 정의
│   ├── kafka-schemas/              # Kafka 메시지 스키마 (JSON Schema)
│   └── libs/                       # 공유 유틸리티
├── docker-compose.yml              # 로컬 개발 환경 (Phase 1)
├── Makefile                        # 공통 빌드/테스트 명령어
└── scripts/
    ├── setup-local.sh              # 로컬 개발환경 셋업
    ├── seed-data.sh                # 테스트 데이터 시딩
    └── ai-dispatch.sh              # AI Agent 태스크 발행
```

---

## 10. 의사결정 확정 사항

| # | 항목 | 결정 | 비고 |
|---|------|------|------|
| 1 | AI Agent 구동 | Kiro (Claude Opus 4.6) | Kiro IDE 내에서 직접 개발 수행 |
| 2 | 배포 인프라 | AWS EC2 | 개발 완료 후 인간이 직접 구축 |
| 3 | 메신저 | 카카오톡 (알림톡/챗봇) | 한국 사용자 최우선 접근 |
| 4 | 주식 데이터 소스 | 한국투자증권 OpenAPI | 국내주식 우선 → 해외주식 확장 |
| 5 | Agent 오케스트레이션 | GitHub Actions | MVP 단계, 이후 확장 검토 |
| 6 | DB 호스팅 | AWS RDS (PostgreSQL) | 관리형으로 운영 부담 최소화 |

---

## 11. 카카오톡 챗봇 연동 아키텍처 상세

### 11.1 카카오 i 오픈빌더 구조

```
┌─────────────────────────────────────────────────────────┐
│                카카오 i 오픈빌더                           │
│                                                          │
│  ┌────────────┐    ┌─────────────┐    ┌──────────────┐  │
│  │ 시나리오    │    │  블록(Block) │    │  스킬(Skill) │  │
│  │ (대화흐름)  │    │  (응답 단위) │    │  (외부 API)  │  │
│  └────────────┘    └─────────────┘    └──────┬───────┘  │
└──────────────────────────────────────────────┼──────────┘
                                               │ REST API 호출
                                               ▼
┌─────────────────────────────────────────────────────────┐
│              Webhook Gateway (우리 서버)                   │
│                                                          │
│  POST /kakao/skill                                       │
│  - 사용자 발화(utterance) 파싱                             │
│  - 의도(intent) 분류                                      │
│  - 비즈니스 로직 호출                                      │
│  - 카카오 응답 포맷(SimpleText/Card/Carousel) 생성         │
└─────────────────────────────────────────────────────────┘
```

### 11.2 카카오 챗봇 시나리오 설계

| 사용자 발화 예시 | 의도(Intent) | 처리 서비스 | 응답 형태 |
|------------------|-------------|-------------|-----------|
| "포트폴리오 등록" | portfolio.register | User Account Service | 종목 입력 폼 |
| "삼성전자 100주 추가" | portfolio.add_stock | User Account Service | 확인 카드 |
| "이번 달 배당금" | dividend.monthly | Dividend Engine | 캘린더 카드 |
| "다음 배당락일" | dividend.ex_date | Dividend Engine | 리스트 카드 |
| "ISA 한도 확인" | account.isa_limit | User Account Service | SimpleText |
| "절세 전략 알려줘" | strategy.tax_optimization | Dividend Engine | Carousel |

### 11.3 카카오 알림톡 (푸시 알림)

```
알림톡 발송 조건:
1. 배당락일 D-3: "삼성전자 배당락일이 3일 후입니다. 현재 보유: 100주"
2. 배당락일 D-1: "내일 배당락일! 배당금 예상: 36,100원"
3. 배당금 입금일: "삼성전자 배당금 36,100원이 입금 예정입니다"
4. ISA 한도 알림: "ISA 비과세 한도 잔여: 50만원 (배당 수령 후 초과 위험)"
5. 월간 리포트: "6월 총 배당금: 152,300원 | YTD: 890,000원"

발송 채널: 카카오 비즈니스 → 알림톡 API
필요 조건: 사업자 등록, 카카오 비즈니스 채널 개설, 템플릿 사전 승인
```

---

## 12. 한국투자증권 OpenAPI 연동 상세

### 12.1 필요 API 목록

| API | 용도 | 호출 주기 |
|-----|------|-----------|
| 국내주식 배당금 조회 | 종목별 배당금액, 배당수익률 | 일 1회 |
| 국내주식 배당락일 조회 | 배당락일 스케줄 | 일 1회 |
| 국내주식 현재가 조회 | 포트폴리오 평가액 계산 | 사용자 요청 시 |
| 해외주식 배당금 조회 (Phase 3) | 미국주식 배당 정보 | 일 1회 |

### 12.2 인증 흐름

```
1. 한국투자증권 개발자 포털 회원가입
2. App Key / App Secret 발급
3. OAuth 2.0 토큰 발급 (POST /oauth2/tokenP)
4. Access Token으로 API 호출 (유효기간: 24시간)
5. Token 만료 시 자동 재발급 로직 구현

주의사항:
- 실전투자/모의투자 환경 분리
- 초당 호출 제한: 20건/초
- 일일 호출 제한: 확인 필요 (계약에 따라 상이)
```

### 12.3 데이터 수집 전략

```
[국내주식 배당 데이터 수집 파이프라인]

매일 02:00 KST
├── 1. Token 재발급 (만료 체크)
├── 2. KOSPI/KOSDAQ 배당 종목 리스트 조회
├── 3. 종목별 배당 정보 수집 (batch, rate limit 준수)
│   ├── 배당금액 (주당)
│   ├── 배당수익률
│   ├── 배당기준일
│   ├── 배당락일
│   └── 배당지급일
├── 4. MinIO에 원본 JSON 저장 (감사 추적용)
├── 5. PostgreSQL에 정제된 데이터 upsert
└── 6. Kafka topic: market.dividend.updated 발행

확장 (Phase 3):
├── 해외주식 배당 정보 추가 수집
├── 환율 정보 수집 (원화 환산)
└── 배당 히스토리 축적 (연간 추세 분석용)
```

---

## 13. 다음 단계 (Immediate Actions)

```
□ 1. GitHub 리포지토리 생성 & 기본 구조 세팅
□ 2. 카카오 비즈니스 채널 개설 & 챗봇 오픈빌더 설정
□ 3. 한국투자증권 개발자 포털 가입 & API Key 발급
□ 4. Planner AI 시스템 프롬프트 & PRD 템플릿 작성
□ 5. Webhook Gateway MVP 개발 (카카오 스킬 서버)
□ 6. Docker Compose로 로컬 개발 환경 구축
□ 7. GitHub Actions CI 워크플로우 기본 세팅
```
