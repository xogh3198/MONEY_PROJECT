# Planner: Phase 4 개선 방향 제안

## 현재 완성도 (Phase 1~3 완료)

### ✅ 구현 완료
- dividend-engine: 배당금 계산, 포트폴리오 CRUD, ISA 누적 추적, 단위 테스트
- webhook-gateway: 카카오 스킬 서버, Intent 라우팅, dividend-engine WebClient 연동
- notification: 카카오 알림톡 발송 서비스 (야간 차단, 발송 이력)
- admin-dashboard: 통계, 알림이력, 시스템상태, 개발현황 페이지
- 한투 API: 토큰 관리, Rate Limit, 재시도 로직
- 스케줄러: 매일 02:00 데이터 수집, 08:00 알림 트리거
- CI/CD: GitHub Actions
- Swagger: API 문서 자동 생성
- 하네스: steering, agents, hooks, memory 전체 구조

### 🟡 기술 부채
- W-001: ExDividendAlertScheduler findAll() → 쿼리 최적화 필요
- W-002: 카카오 알림톡 실제 API 호출 미구현 (로깅만)
- notification 서비스 테스트 없음
- 통합 테스트 (Testcontainers) 없음

## Phase 4 제안 (우선순위 순)

| # | 태스크 | 가치 | 복잡도 |
|---|--------|------|--------|
| 1 | 통합 테스트 (Testcontainers + PostgreSQL) | 높음 | M |
| 2 | PortfolioRepository.findByStockCode() 쿼리 추가 | 높음 | S |
| 3 | 사용자 인증: 카카오 OAuth → JWT 토큰 발급 | 높음 | L |
| 4 | 카카오 알림톡 실제 API 연동 (sender key 설정) | 중간 | M |
| 5 | API Rate Limiter (Redis + Token Bucket) | 중간 | M |
| 6 | 배당 히스토리 축적 (연간 추세 분석용) | 낮음 | S |
| 7 | Kafka 이벤트 아키텍처 전환 (서비스 간 비동기) | 낮음 | L |

## 인간 결정 필요
- Phase 4를 바로 시작할지, 현재 완성도에서 배포 테스트를 먼저 할지
- 카카오 비즈니스 채널 개설 + API Key 발급 상태 확인
- 한투 API App Key 발급 상태 확인
