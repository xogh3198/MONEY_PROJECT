# Harness Engineering 적용 전략

> 참조: [revfactory/harness](https://github.com/revfactory/harness) — Claude Code Agent Team Architecture Factory

## 1. 하네스 엔지니어링이란?

AI에게 **가이드라인으로 방향성을 맞춰주는** 시스템 설계 방법론입니다.
모델을 바꾸지 않고, 모델을 감싸는 환경(에이전트 정의, 스킬, 오케스트레이션)을
구조화해서 AI의 출력 품질을 최적화합니다.

**핵심 개념 (revfactory/harness 기반):**
- **에이전트 정의**: "누가" 하는가 — 역할, 원칙, 입출력 프로토콜
- **스킬**: "어떻게" 하는가 — 작업 절차, 규칙, 참조 문서
- **오케스트레이터**: "언제, 어떤 순서로" 하는가 — 팀 조율, 데이터 흐름

**효과:** 동일 모델에서 평균 품질 +60%, 15/15 승률, 분산 -32% 달성
(revfactory/claude-code-harness A/B 테스트 결과)

---

## 2. 아키텍처 패턴 선택

revfactory/harness가 제공하는 6가지 패턴 중,
배당금 봇 프로젝트에는 **Producer-Reviewer + Pipeline** 하이브리드가 적합합니다.

| 패턴 | 설명 | 우리 프로젝트 적용 |
|------|------|-------------------|
| Pipeline | 순차 의존 작업 | 기획 → 개발 → 리뷰 → 배포 |
| Fan-out/Fan-in | 병렬 독립 작업 | 여러 Dev AI가 각 서비스를 동시 개발 |
| Expert Pool | 상황별 선택 호출 | 금융 규칙 전문가, API 전문가 등 필요시 호출 |
| **Producer-Reviewer** | 생성 후 품질 검수 | **Dev AI 생성 → Reviewer AI 검증** |
| Supervisor | 중앙 에이전트가 동적 분배 | Planner AI가 태스크 분배 |
| Hierarchical Delegation | 상위→하위 재귀 위임 | 대형 기능을 서브태스크로 분해 |

**우리 선택: Supervisor + Producer-Reviewer 하이브리드**
```
[Planner AI] ──(Supervisor)──→ [Dev AI #1~N] ──(Producer)──→ [Reviewer AI] ──(Reviewer)
     │                              │                              │
     │ 태스크 분배 & 감독            │ 코드 생성                     │ 품질 검증
     └──────────────────────────────┴──────────────────────────────┘
                                Pipeline (순차 흐름)
```

---

## 3. Kiro 환경 매핑

revfactory/harness는 Claude Code의 `.claude/` 구조를 사용하지만,
우리는 **Kiro IDE**를 사용하므로 동일 개념을 Kiro 체계로 매핑합니다.

| revfactory/harness (Claude Code) | Kiro 대응 | 역할 |
|----------------------------------|-----------|------|
| `.claude/agents/{name}.md` | `.kiro/agents/{name}.md` | 에이전트 정의 (역할, 원칙, 프로토콜) |
| `.claude/skills/{name}/SKILL.md` | `.kiro/steering/{name}.md` | 스킬 (작업 절차, 규칙) |
| `CLAUDE.md` (하네스 포인터) | `.kiro/steering/project-context.md` | 프로젝트 맥락 + 트리거 규칙 |
| Agent Teams (`TeamCreate`) | Kiro Sub-agents | 다중 에이전트 위임 |
| `_workspace/` (중간 산출물) | `_workspace/` | 에이전트 간 데이터 전달 |
| Hooks (자동 검증) | `.kiro/hooks/` | 검증 루프 자동화 |

---

## 4. 에이전트 정의

### 4.1 Planner AI (기획자)

```markdown
# .kiro/agents/planner.md
---
name: planner
role: "배당금 봇 서비스 기획자 — PRD 생성 및 태스크 분할"
---

## 핵심 역할
배당금 캘린더 & 절세 최적화 봇의 기획을 담당한다.
인간의 기획 요청을 받아 PRD를 생성하고, 기술 타당성을 검증한 뒤
개발 태스크를 분할하여 Developer AI에게 전달한다.

## 작업 원칙
1. PRD는 반드시 정해진 템플릿 구조를 따른다 (skills/prd-creation 참조)
2. 기술 타당성 검증 없이 개발 태스크를 발행하지 않는다
3. 한국 금융 규제에 대해 불확실한 사항은 "확인 필요"로 명시한다
4. 태스크 분할 시 각 태스크는 독립적으로 실행 가능해야 한다
5. Developer AI 수는 최대 5개로 제한한다 (조율 오버헤드 관리)
6. 투자 "조언" 기능은 기획 단계에서 차단한다 ("정보 제공"만 가능)

## 입력 프로토콜
- 인간의 기획 요청 (자연어)
- 기존 아키텍처 문서 (docs/architecture/)
- 이전 PRD 및 피드백 이력 (memory/)

## 출력 프로토콜
- PRD 문서 → `_workspace/prd/{feature-name}.md`
- 태스크 분할표 → `_workspace/tasks/{feature-name}-tasks.md`
- 기술 타당성 보고 → `_workspace/feasibility/{feature-name}.md`

## 에러 핸들링
- API 가용성 확인 불가 시: "미확인" 명시 후 진행, 인간에게 확인 요청
- 기존 서비스와 충돌 감지 시: 충돌 지점 명시, 대안 제시

## 팀 통신
- Dev AI에게: 태스크 티켓 전달 (입출력 스펙, 테스트 시나리오 포함)
- Reviewer AI에게: 리뷰 기준(PRD 기반) 전달
- 인간에게: PRD 요약 및 확인 요청
```

### 4.2 Developer AI (개발자)

```markdown
# .kiro/agents/developer.md
---
name: developer
role: "배당금 봇 마이크로서비스 개발자 — 코드 생성 및 PR 작성"
---

## 핵심 역할
Planner AI가 분할한 태스크를 받아 실제 코드를 작성하고
feature branch에 커밋하여 PR을 생성한다.

## 작업 원칙
1. 담당 서비스 범위 내에서만 코드를 수정한다
2. 코드 작성 전 반드시 관련 스킬 파일을 확인한다
3. 금액 계산에 float를 절대 사용하지 않는다 (decimal 라이브러리 필수)
4. 세율은 하드코딩 금지 — config/tax_rates.yaml에서 로드
5. 모든 public 함수에 단위 테스트를 작성한다 (경계값 포함)
6. 빌드 + 린트 + 테스트 통과 확인 후에만 PR을 생성한다
7. 서비스 간 통신은 Kafka 토픽 또는 gRPC만 사용한다

## 입력 프로토콜
- Planner AI의 태스크 티켓 (`_workspace/tasks/`)
- API 명세 (`docs/api-specs/`)
- 관련 스킬 파일 (`.kiro/steering/`)

## 출력 프로토콜
- 코드 → `services/{service-name}/` (feature branch)
- PR 생성 → GitHub
- 구현 노트 → `_workspace/impl-notes/{task-id}.md`

## 에러 핸들링
- 빌드 실패: 에러 메시지 분석 후 자체 수정 (최대 3회)
- 테스트 실패: 실패 원인 분석, 로직 수정 후 재실행
- 외부 API 스펙 불명확: Planner에게 명확화 요청

## 셀프 검증 (PR 생성 전 필수)
- [ ] go build ./... 성공
- [ ] golangci-lint run 통과
- [ ] go test ./... -v 전체 통과
- [ ] 금융 계산 함수에 경계값 테스트 포함
- [ ] API Key/시크릿 하드코딩 없음
- [ ] README 업데이트 (신규 엔드포인트 추가 시)
```

### 4.3 Reviewer AI (리뷰어)

```markdown
# .kiro/agents/reviewer.md
---
name: reviewer
role: "PR 코드 리뷰어 — 품질 검증 및 이슈 분류"
---

## 핵심 역할
Developer AI가 생성한 PR을 검증하고,
이슈를 심각도별로 분류하여 인간에게 요약 리포트를 전달한다.

## 작업 원칙
1. 변경된 코드와 직접 관련된 부분만 리뷰한다
2. 코드 스타일은 린터가 이미 검증했으므로 로직/설계에 집중한다
3. 확실하지 않은 이슈는 "suggestion"으로 표시한다 (FP 최소화)
4. 금융 계산 로직 변경은 반드시 세율/반올림 정확성을 검증한다
5. Critical 이슈 발견 시 반드시 Request Changes 판정한다

## 심각도 분류 기준
- **Critical**: 금융 계산 오류, 보안 취약점, 데이터 유실 가능성
- **Warning**: 성능 이슈, 에러 핸들링 미흡, 테스트 부족
- **Info**: 네이밍 개선, 리팩토링 제안, 문서화 보완

## 필수 체크 항목 (금융 서비스 특화)
- 배당금 계산: 세율 적용 정확성 (15.4% 일반 / 9.9% ISA 초과분)
- 금액 타입: decimal 사용 여부 (float 절대 금지)
- API Rate Limit: 한투 API 20건/초 준수 로직
- Kafka 메시지: 멱등성(idempotency) 보장
- DB 쿼리: Parameterized Query, N+1 패턴 탐지

## 출력 프로토콜 (인간에게 전달)
```
## PR 리뷰 요약
- 변경: {n}개 파일, +{additions}/-{deletions} 라인
- Critical: {count}건
- Warning: {count}건
- 판정: Approve / Request Changes
- 핵심: {1줄 요약}
```

## 에러 핸들링
- 코드 컨텍스트 부족 시: 관련 파일 추가 읽기 후 재평가
- Planner PRD와 불일치 감지 시: 불일치 사항 명시, Dev에게 확인 요청
```

---

## 5. 스킬 시스템 (Steering Files)

revfactory/harness의 스킬 = Kiro의 steering 파일.
**Progressive Disclosure** 원칙을 적용합니다:

| 단계 | 로딩 시점 | 크기 | Kiro 구현 |
|------|----------|------|-----------|
| Metadata | 항상 | ~100단어 | steering 파일 front-matter |
| 본문 | 관련 작업 시 | <500줄 | `inclusion: fileMatch` 조건부 포함 |
| References | 필요할 때만 | 무제한 | `#[[file:...]]` 참조 |

### 5.1 핵심 스킬 (항상 포함)

```markdown
# .kiro/steering/project-context.md (항상 포함)
---
inclusion: always
---

## 배당금 캘린더 & 절세 최적화 봇

### 프로젝트 개요
카카오톡 챗봇 기반 배당금 관리 서비스.
한국투자증권 OpenAPI로 배당 데이터를 수집하고,
ISA/IRP 계좌 절세 최적화 알림을 카카오 알림톡으로 발송한다.

### 기술 스택
- 백엔드: Go 1.22+ (주력), Python 3.12 (데이터 파이프라인)
- DB: AWS RDS PostgreSQL 16
- 메시지 큐: Apache Kafka (KRaft)
- 배포: AWS EC2 + K3s + ArgoCD
- 메신저: 카카오톡 (챗봇 + 알림톡)
- 데이터: 한국투자증권 OpenAPI (국내 → 해외 확장)

### 에이전트 트리거 규칙
- 기획/PRD 관련 요청 → Planner 에이전트 스킬 사용
- 코드 구현 요청 → Developer 에이전트 스킬 사용
- PR 리뷰 요청 → Reviewer 에이전트 스킬 사용

### 변경 이력
| 날짜 | 변경 내용 | 대상 | 사유 |
|------|----------|------|------|
| 2026-06-17 | 초기 구성 | 전체 | 프로젝트 시작 |
```

### 5.2 금융 도메인 스킬 (항상 포함)

```markdown
# .kiro/steering/financial-rules.md
---
inclusion: always
---

## 금융 계산 규칙 (절대 위반 금지)

### 세율 규정 (2024년 기준)
- 배당소득 원천징수: 15.4% (소득세 14% + 지방소득세 1.4%)
- ISA 비과세 한도: 일반형 200만원, 서민형 400만원
- ISA 비과세 초과분: 9.9% 분리과세
- IRP: 퇴직 시점까지 과세이연, 수령 시 3.3~5.5%
- 해외주식 배당: 현지 원천징수 15% (미국 기준)

### 금액 계산 필수 규칙
1. float 타입 사용 절대 금지 → Go: shopspring/decimal, Python: decimal.Decimal
2. 세율은 하드코딩 금지 → config/tax_rates.yaml에서 로드
3. 반올림: 원 단위 이하 절사 (한국 금융 관행)
4. 모든 계산 함수에 테이블 드리븐 테스트 작성
5. 경계값 테스트 필수: 비과세 한도 직전, 한도 도달, 한도 초과

### ISA 계좌 규칙
- 연간 납입 한도: 2,000만원 (2024년 기준)
- 의무 가입 기간: 3년
- 비과세 한도: 일반 200만원, 서민형/농어민 400만원
- 한도 초과 시: 초과분에 9.9% 분리과세 (종합과세 아님)

### 배당락일 규칙
- 배당락일: 배당기준일 1영업일 전 (T+2 결제 기준)
- 배당락일 당일 매수: 배당금 수령 불가
- 배당락일 전일까지 매수 완료: 배당금 수령 가능
- 공휴일/주말 고려한 영업일 계산 필수

### 면책 규정
- 모든 사용자 대면 메시지에 포함: "본 정보는 투자 조언이 아닙니다"
- 투자 판단에 대한 책임은 사용자에게 있음을 명시
```

### 5.3 Go 서비스 패턴 스킬 (Go 파일 작업 시 포함)

```markdown
# .kiro/steering/go-patterns.md
---
inclusion: fileMatch
fileMatchPattern: "**/*.go"
---

## Go 마이크로서비스 패턴

### 프로젝트 구조 (서비스별)
services/{service-name}/
├── cmd/main.go           # 진입점: config 로드, DI, 서버 시작
├── internal/
│   ├── handler/          # HTTP/gRPC 핸들러 (입력 검증만)
│   ├── service/          # 비즈니스 로직 (핵심)
│   ├── repository/       # DB 접근 (인터페이스 기반)
│   └── domain/           # 도메인 모델, 값 객체
├── pkg/                  # 외부 공개 패키지 (최소화)
├── Dockerfile
└── go.mod

### 구현 순서 (반드시 이 순서)
1. domain/ — 도메인 모델, 값 객체 정의
2. repository/ — 인터페이스 정의 (구현은 나중)
3. service/ — 비즈니스 로직 (repository 인터페이스에 의존)
4. handler/ — HTTP/gRPC 핸들러 (service에 위임만)
5. 단위 테스트 — service 레이어 중심, mock repository
6. repository 구현 — PostgreSQL 구현체
7. 통합 테스트 — 실제 DB 사용

### 필수 패턴
- DI: 생성자 주입 (interface 기반), 글로벌 변수 금지
- 에러: fmt.Errorf("%w", err)로 wrapping, sentinel error 정의
- 로깅: zerolog (structured), 금융 데이터 마스킹 필수
- 설정: viper (환경변수 우선, YAML fallback)
- 금액: shopspring/decimal (float 절대 금지)
- HTTP: chi 라우터, middleware 체인
- 테스트: testify + mockery (interface mock 자동 생성)

### 헬스체크 (모든 서비스 필수)
- GET /health — liveness (항상 200)
- GET /ready — readiness (DB/Kafka 연결 확인)
```

### 5.4 카카오 API 스킬 (카카오 관련 코드 작업 시)

```markdown
# .kiro/steering/kakao-api-guide.md
---
inclusion: fileMatch
fileMatchPattern: "**/kakao/**,**/notification/**"
---

## 카카오톡 챗봇 & 알림톡 연동 가이드

### 카카오 i 오픈빌더 스킬 서버 규격
- 엔드포인트: POST /kakao/skill
- 요청 형식: SkillPayload (userRequest, bot, action)
- 응답 형식: SkillResponse (template: SimpleText, Card, Carousel)
- 응답 시간 제한: 5초 이내 (초과 시 타임아웃 에러)

### 알림톡 발송 규칙
- 사전 승인된 템플릿만 사용 가능 (자유 형식 불가)
- 템플릿 변수: #{이름}, #{종목명}, #{배당금} 등
- 발송 시간: 08:00~21:00 (야간 발송 금지)
- 수신 동의 확인 필수 (정보통신망법)

### 응답 포맷 예시
```json
{
  "version": "2.0",
  "template": {
    "outputs": [
      {
        "simpleText": {
          "text": "이번 달 예상 배당금: 152,300원\n- 삼성전자: 36,100원\n- SK하이닉스: 12,200원"
        }
      }
    ]
  }
}
```
```

### 5.5 한국투자증권 API 스킬 (한투 API 코드 작업 시)

```markdown
# .kiro/steering/kis-api-guide.md
---
inclusion: fileMatch
fileMatchPattern: "**/market-data/**,**/kis/**"
---

## 한국투자증권 OpenAPI 연동 가이드

### 인증
- OAuth 2.0: POST /oauth2/tokenP
- App Key + App Secret → Access Token (유효: 24시간)
- 만료 시 자동 재발급 로직 필수

### Rate Limit (절대 준수)
- 초당 20건 제한
- Token Bucket 패턴으로 호출 속도 제어
- 제한 초과 시: 429 응답, 1초 대기 후 재시도 (최대 3회)

### 주요 API
| API | 경로 | 용도 |
|-----|------|------|
| 국내 배당금 조회 | /uapi/domestic-stock/v1/quotations/... | 종목별 배당 정보 |
| 현재가 조회 | /uapi/domestic-stock/v1/quotations/inquire-price | 포트폴리오 평가 |
| 해외 배당 (Phase 3) | /uapi/overseas-stock/v1/... | 미국주식 배당 |

### 데이터 수집 규칙
- 수집 주기: 매일 02:00 KST (1회/일이면 충분)
- 원본 JSON: MinIO에 캐시 저장 (감사 추적용)
- 정제 데이터: PostgreSQL에 upsert
- 실패 시: 3회 재시도, 이후 이전 캐시 데이터 사용
```

---

## 6. 오케스트레이터 스킬

에이전트 팀 전체를 조율하는 메타 스킬입니다.
revfactory/harness의 Phase 5 (Integration & Orchestration) 개념.

```markdown
# .kiro/steering/orchestrator.md
---
inclusion: manual
---

## 배당금 봇 오케스트레이터

### 트리거
"기능 개발", "신규 기능", "서비스 추가", "개발 시작" 등의 요청 시 이 스킬을 사용한다.

### 실행 모드: Supervisor + Producer-Reviewer 하이브리드

### Phase 0: 컨텍스트 확인
1. `_workspace/` 존재 여부 확인
   - 미존재 → 초기 실행 (Phase 1부터)
   - 존재 + 부분 수정 요청 → 해당 Phase만 재실행
   - 존재 + 새 입력 → 기존을 `_workspace_prev/`로 이동, 새 실행
2. 기존 서비스 목록 확인 (services/ 디렉토리)
3. 진행 중인 태스크 확인 (GitHub Issues/PRs)

### Phase 1: 기획 (Planner AI)
**실행:** Planner 에이전트에 기획 요청 위임
**입력:** 인간의 기획 요청
**출력:** 
- `_workspace/prd/{feature}.md`
- `_workspace/tasks/{feature}-tasks.md`
**완료 조건:** PRD 필수 섹션 모두 존재, 태스크 의존성 순환 없음

### Phase 2: 개발 (Developer AI × N)
**실행:** 태스크별 Developer 에이전트에 병렬 위임 (Fan-out)
**입력:** 각 태스크 티켓 + 관련 스킬 파일
**출력:**
- `services/{service}/` 코드
- PR 생성 (feature branch)
- `_workspace/impl-notes/{task-id}.md`
**완료 조건:** 빌드 성공 + 린트 통과 + 테스트 통과

### Phase 3: 리뷰 (Reviewer AI)
**실행:** 각 PR에 대해 Reviewer 에이전트 위임
**입력:** PR diff + PRD + 금융 규칙 스킬
**출력:**
- PR 코멘트
- 인간용 리뷰 요약 (`_workspace/reviews/{pr-number}.md`)
**완료 조건:** Critical 이슈 0건 → Approve, 있으면 → Request Changes

### Phase 4: 인간 승인
**실행:** 인간에게 리뷰 요약 전달, 승인 대기
**승인 시:** Merge → CD 파이프라인 트리거
**반려 시:** 피드백을 Planner에게 전달 → Phase 1 또는 Phase 2로 회귀

### 데이터 흐름
```
Human Request
    → [Phase 1] _workspace/prd/ + _workspace/tasks/
        → [Phase 2] services/{name}/ + PR + _workspace/impl-notes/
            → [Phase 3] _workspace/reviews/ + PR comments
                → [Phase 4] Human Decision → Merge or Feedback Loop
```

### 에러 핸들링
- Phase 2 빌드 실패: Developer가 자체 수정 (최대 3회), 이후 인간 개입
- Phase 3 Critical 발견: 자동 Request Changes, Developer에게 수정 요청
- 동일 이슈 2회 반복: 해당 패턴을 메모리에 기록, 스킬 가드레일 추가
- 전체 실패: 인간에게 에스컬레이션 + 실패 원인 분석 보고

### 팀 크기 가이드
| 기능 규모 | Dev AI 수 | 예시 |
|-----------|-----------|------|
| 소 (1~2 서비스) | 1~2 | 알림 서비스 수정 |
| 중 (3~4 서비스) | 2~3 | 카카오 챗봇 연동 |
| 대 (5+ 서비스) | 3~5 | 전체 MVP 구축 |
```

---

## 7. 검증 & 피드백 루프

revfactory/harness Phase 6 (Validation & Testing) + Phase 7 (Evolution) 적용.

### 7.1 Kiro Hooks로 자동 검증

```json
// .kiro/hooks/go-lint.json — Go 파일 수정 시 자동 린트
{
  "name": "Go Lint Check",
  "version": "1.0.0",
  "when": { "type": "fileEdited", "patterns": ["*.go"] },
  "then": { "type": "runCommand", "command": "golangci-lint run ./..." }
}

// .kiro/hooks/go-test.json — Go 파일 수정 시 자동 테스트
{
  "name": "Go Test Run",
  "version": "1.0.0",
  "when": { "type": "fileEdited", "patterns": ["services/**/*.go"] },
  "then": { "type": "runCommand", "command": "go test ./..." }
}

// .kiro/hooks/finance-guardrail.json — 코드 작성 시 금융 규칙 체크
{
  "name": "Finance Logic Guard",
  "version": "1.0.0",
  "when": { "type": "preToolUse", "toolTypes": ["write"] },
  "then": {
    "type": "askAgent",
    "prompt": "이 코드에 금융 계산이 포함되어 있다면 확인하세요: 1) float 대신 decimal 사용 2) 세율 하드코딩 여부 3) 경계값 테스트 존재. 금융 계산이 없으면 무시하세요."
  }
}

// .kiro/hooks/post-task-review.json — Spec 태스크 완료 시 자동 리뷰
{
  "name": "Auto Review After Task",
  "version": "1.0.0",
  "when": { "type": "postTaskExecution" },
  "then": {
    "type": "askAgent",
    "prompt": "방금 완료한 태스크의 코드를 Reviewer 에이전트 기준으로 셀프 리뷰하세요. Critical 이슈가 있으면 즉시 수정하세요."
  }
}
```

### 7.2 실패 분류 & 하네스 진화

revfactory/harness Phase 7의 핵심:
**실패할 때마다 하네스를 업데이트하여 같은 실패가 반복되지 않게 한다.**

| 실패 유형 | 증상 | 수정 대상 |
|-----------|------|-----------|
| 컨텍스트 실패 | AI가 잘못된 API를 호출하는 코드 생성 | steering 파일에 API 정보 추가 |
| 가드레일 위반 | float로 배당금 계산 | financial-rules.md 강화 |
| 검증 실패 | 세율 9.9%를 15.4%로 잘못 적용 | 테스트 케이스 추가, Hook 강화 |
| 계획 실패 | 하나의 거대한 함수로 모든 기능 구현 | 스킬에 분할 기준 추가 |
| 트리거 실패 | 특정 표현으로 에이전트가 작동 안 함 | steering description 확장 |

**진화 프로세스:**
```
실패 발생 → 유형 분류 → 해당 steering/hook 파일 수정 → 동일 입력 재테스트 → 성공 확인
     ↓                                                              ↓
  변경 이력 기록 (project-context.md)                        메모리에 패턴 저장
```

---

## 8. 메모리 시스템 (Cross-Session Learning)

revfactory/harness의 `_workspace/` + Phase 7 피드백 축적을 구현합니다.

```
_workspace/                          # 에이전트 간 데이터 전달 (실행 중)
├── prd/                             # Planner 산출물
├── tasks/                           # 태스크 분할표
├── impl-notes/                      # Developer 구현 노트
├── reviews/                         # Reviewer 리뷰 결과
└── feasibility/                     # 기술 타당성 보고

ai-agents/memory/                    # 세션 간 학습 축적 (영구)
├── project-decisions.md             # 확정된 기술 결정
├── rejected-approaches.md           # 실패한 접근법 기록
├── human-feedback-log.md            # 인간 피드백 이력
├── common-bugs.md                   # 반복 버그 패턴
├── api-limitations.md               # 발견된 API 제약
└── harness-evolution-log.md         # 하네스 수정 이력
```

### 메모리 업데이트 트리거

| 이벤트 | 메모리 업데이트 |
|--------|----------------|
| Human이 PR 반려 | human-feedback-log.md + 해당 steering 파일 보강 |
| 동일 버그 2회 발생 | common-bugs.md에 패턴 등록, steering 가드레일 추가 |
| API 제한 발견 | api-limitations.md 추가, kis-api-guide.md 보강 |
| 기획 방향 변경 | project-decisions.md 업데이트 |
| 하네스 파일 수정 | harness-evolution-log.md에 날짜/사유 기록 |

---

## 9. 프로젝트 디렉토리 구조 (하네스 관련)

```
dividend-bot/
├── .kiro/
│   ├── steering/                        # 스킬 시스템 (= .claude/skills/)
│   │   ├── project-context.md           # 프로젝트 맥락 + 트리거 규칙 (always)
│   │   ├── financial-rules.md           # 금융 도메인 규칙 (always)
│   │   ├── coding-standards.md          # 코드 규칙 (always)
│   │   ├── go-patterns.md              # Go 패턴 (fileMatch: *.go)
│   │   ├── python-patterns.md          # Python 패턴 (fileMatch: *.py)
│   │   ├── kakao-api-guide.md          # 카카오 API (fileMatch)
│   │   ├── kis-api-guide.md            # 한투 API (fileMatch)
│   │   └── orchestrator.md             # 오케스트레이터 (manual)
│   ├── hooks/                           # 검증 루프 자동화
│   │   ├── go-lint.json
│   │   ├── go-test.json
│   │   ├── finance-guardrail.json
│   │   └── post-task-review.json
│   ├── agents/                          # 에이전트 정의 (= .claude/agents/)
│   │   ├── planner.md
│   │   ├── developer.md
│   │   └── reviewer.md
│   └── specs/                           # Kiro Spec (구조화된 태스크)
│
├── _workspace/                          # 에이전트 간 산출물 (실행 중)
│   ├── prd/
│   ├── tasks/
│   ├── impl-notes/
│   └── reviews/
│
├── ai-agents/memory/                    # 세션 간 학습 (영구 축적)
│   ├── project-decisions.md
│   ├── human-feedback-log.md
│   ├── common-bugs.md
│   └── harness-evolution-log.md
│
├── config/
│   └── tax_rates.yaml                   # 세율 설정 (하드코딩 방지)
│
└── services/                            # 실제 서비스 코드
```

---

## 10. 도입 로드맵

### 즉시 실행 (Phase 1과 동시)
```
□ .kiro/steering/ 핵심 3개 파일 생성
  - project-context.md
  - financial-rules.md  
  - coding-standards.md
□ .kiro/agents/ 3개 에이전트 정의
  - planner.md, developer.md, reviewer.md
□ .kiro/hooks/ 기본 검증 훅 설정
□ config/tax_rates.yaml 생성
□ _workspace/ 디렉토리 구조 세팅
```

### 개발 진행하며 (Phase 2~3)
```
□ 서비스별 steering 파일 추가 (kakao, kis-api)
□ 실패 발생 시 즉시 steering 파일 보강
□ 메모리 시스템 운영 시작
□ 하네스 진화 로그 기록
```

### 안정화 (지속)
```
□ 반복 패턴 발견 시 스킬 파일에 번들링
□ 주간 하네스 현황 감사 (Phase 0 실행)
□ 하네스 효과 측정 (동일 태스크 재실행 품질 비교)
```

---

## 11. 핵심 원칙 요약

```
revfactory/harness 핵심 원칙 → 배당금 봇 적용

1. 에이전트(누가) ↔ 스킬(어떻게) 분리
   → .kiro/agents/ ↔ .kiro/steering/ 분리

2. Progressive Disclosure (점진적 정보 공개)
   → steering의 inclusion 조건 (always / fileMatch / manual)

3. 실패 시 하네스 진화 (같은 실수 구조적 방지)
   → steering 파일 즉시 보강 + hooks 추가

4. 팀 아키텍처 패턴 기반 설계
   → Supervisor(Planner) + Producer-Reviewer(Dev-Reviewer)

5. 검증은 자동화, 결과는 수치화
   → hooks로 린트/테스트/가드레일 자동 실행

6. 모든 변경은 이력 추적
   → project-context.md 변경 이력 + harness-evolution-log
```
