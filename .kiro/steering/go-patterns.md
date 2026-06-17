---
inclusion: fileMatch
fileMatchPattern: "**/*.go"
---

## Go 마이크로서비스 패턴

### 프로젝트 구조 (서비스별)
```
services/{service-name}/
├── cmd/main.go              # 진입점: config 로드, DI 초기화, 서버 시작
├── internal/
│   ├── handler/             # HTTP/gRPC 핸들러 (입력 검증, 응답 포맷팅)
│   ├── service/             # 비즈니스 로직 (핵심 도메인 로직)
│   ├── repository/          # DB 접근 (인터페이스 + 구현체)
│   └── domain/              # 도메인 모델, 값 객체, enum
├── pkg/                     # 외부 공개 패키지 (최소화)
├── Dockerfile
├── go.mod
└── README.md
```

### 구현 순서 (반드시 이 순서)
1. `domain/` — 도메인 모델, 값 객체 정의
2. `repository/` — 인터페이스 정의 (구현은 나중에)
3. `service/` — 비즈니스 로직 (repository 인터페이스에 의존)
4. `handler/` — HTTP/gRPC 핸들러 (service에 위임)
5. 단위 테스트 — service 레이어 중심, mock repository
6. `repository/` 구현 — PostgreSQL 구현체
7. 통합 테스트 — 실제 DB 사용 (testcontainers)

### 생성자 패턴
```go
type DividendService struct {
    repo   DividendRepository
    config *Config
    logger zerolog.Logger
}

func NewDividendService(repo DividendRepository, cfg *Config, logger zerolog.Logger) *DividendService {
    return &DividendService{repo: repo, config: cfg, logger: logger}
}
```

### 에러 처리 패턴
```go
// sentinel error 정의
var (
    ErrStockNotFound     = errors.New("stock not found")
    ErrISALimitExceeded  = errors.New("ISA tax-free limit exceeded")
)

// wrapping으로 컨텍스트 추가
func (s *DividendService) Calculate(stockCode string) (decimal.Decimal, error) {
    stock, err := s.repo.FindByCode(stockCode)
    if err != nil {
        return decimal.Zero, fmt.Errorf("finding stock %s: %w", stockCode, err)
    }
    // ...
}
```

### 금액 계산 패턴 (shopspring/decimal)
```go
import "github.com/shopspring/decimal"

func CalculateAfterTax(pretax decimal.Decimal, taxRate decimal.Decimal) decimal.Decimal {
    tax := pretax.Mul(taxRate)
    afterTax := pretax.Sub(tax)
    return afterTax.Floor() // 원 단위 이하 절사
}
```

### 테스트 패턴 (테이블 드리븐)
```go
func TestCalculateAfterTax(t *testing.T) {
    tests := []struct {
        name     string
        pretax   string
        taxRate  string
        expected string
    }{
        {"일반 배당", "36100", "0.154", "30540"},
        {"ISA 초과분", "50000", "0.099", "45050"},
        {"영(0) 배당", "0", "0.154", "0"},
    }
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            pretax := decimal.RequireFromString(tt.pretax)
            taxRate := decimal.RequireFromString(tt.taxRate)
            expected := decimal.RequireFromString(tt.expected)
            result := CalculateAfterTax(pretax, taxRate)
            assert.True(t, result.Equal(expected))
        })
    }
}
```

### Kafka Consumer 패턴
```go
func (c *Consumer) HandleMessage(ctx context.Context, msg *kafka.Message) error {
    // 1. 메시지 역직렬화
    // 2. 멱등성 체크 (이미 처리된 메시지인지)
    // 3. 비즈니스 로직 실행
    // 4. 성공 시 오프셋 커밋
    // 5. 실패 시 DLQ(Dead Letter Queue)로 전송
}
```

### 헬스체크 (모든 서비스 필수)
```go
// GET /health — liveness (항상 200)
r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
})

// GET /ready — readiness (의존성 확인)
r.Get("/ready", func(w http.ResponseWriter, r *http.Request) {
    if err := db.PingContext(r.Context()); err != nil {
        w.WriteHeader(http.StatusServiceUnavailable)
        return
    }
    w.WriteHeader(http.StatusOK)
})
```
