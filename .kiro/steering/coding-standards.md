---
inclusion: always
---

## 코딩 표준

### Go 서비스 (주력)
- 스타일: uber-go/guide 준수, gofmt 필수
- 린트: golangci-lint (설정: .golangci.yml)
- 에러 처리: fmt.Errorf("%w", err)로 wrapping, sentinel error 정의
- DI: 생성자 주입 (interface 기반), 글로벌 변수 금지
- 로깅: zerolog (structured JSON logging)
- 설정: viper (환경변수 우선, YAML fallback)
- 테스트: testify + mockery (interface mock 자동 생성)
- 금액: shopspring/decimal (float 절대 금지)
- HTTP: chi 라우터 + middleware 체인
- 함수 길이: 50줄 이내 권장, 100줄 초과 금지

### Python 서비스 (데이터 파이프라인)
- 포맷: Black + isort
- 타입: mypy strict mode
- 금액: decimal.Decimal (float 절대 금지)
- HTTP: httpx (async)
- 테스트: pytest + pytest-asyncio

### 공통 규칙
- 모든 서비스는 /health (liveness), /ready (readiness) 엔드포인트 필수
- API Key/시크릿은 환경변수에서 로드 (하드코딩 절대 금지)
- 사용자 금융 데이터 로그 출력 금지 (마스킹 필수)
- DB 쿼리는 반드시 Parameterized Query 사용
- 서비스 간 통신: Kafka 토픽 또는 gRPC (직접 HTTP 호출 금지)
- 외부 API 호출 시 Circuit Breaker 패턴 적용
- 모든 public 함수에 GoDoc/docstring 작성

### Git 규칙
- 브랜치: feat/{service}/{task-id} 형식
- 커밋: Conventional Commits (feat:, fix:, docs:, refactor:)
- main 직접 커밋 금지 — PR + 리뷰 필수
- PR 제목: 70자 이내, 변경 내용 한 줄 요약
