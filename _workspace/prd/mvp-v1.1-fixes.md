# PRD v1.1: Reviewer 피드백 반영 패치

## 반영할 Critical 이슈

### C-001 해결: Gradle Wrapper 생성
- 각 서비스에 gradlew, gradle/wrapper/ 파일 추가
- CI 파이프라인 빌드 가능하도록 보장

### C-002 해결: ISA 누적 배당소득 추적
- 신규 엔티티: `DividendAccumulation` — 연간 누적 배당소득 추적
- DividendCalculationService 수정: 개별 비교가 아닌 누적 합산 기준 비과세 판단
- 테이블: user_dividend_accumulation (user_id, year, accumulated_amount)

### C-003 해결: 핵심 단위 테스트 추가
- DividendCalculationService 테이블 드리븐 테스트
- 경계값: ISA 한도 직전(199만), 정확히(200만), 초과(201만)
- 일반 계좌 15.4% 원천징수 정확성 검증
- IRP 과세이연(0%) 검증

## 반영할 Warning 이슈

### W-003 해결: go-patterns.md 삭제 → java-patterns.md 생성
### W-002 해결: admin-dashboard package-lock.json은 별도 (로컬 npm install 필요)
### W-004 해결: init.sql은 JPA 이후 실행되므로 data.sql로 이동

## 태스크 분할

| ID | 태스크 | 복잡도 |
|----|--------|--------|
| FIX-001 | Gradle Wrapper 생성 | S |
| FIX-002 | DividendAccumulation 엔티티 + Repository | S |
| FIX-003 | DividendCalculationService ISA 누적 로직 수정 | M |
| FIX-004 | DividendCalculationService 단위 테스트 작성 | M |
| FIX-005 | go-patterns.md → java-patterns.md 교체 | S |
| FIX-006 | init.sql → data.sql 시드 데이터 분리 | S |
