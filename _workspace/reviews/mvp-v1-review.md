# PR 리뷰 결과 — MVP v1 Initial

## 요약
- **변경**: 62개 파일, +3,856 라인
- **Critical**: 3건
- **Warning**: 5건
- **판정**: ⚠️ Request Changes

---

## 🔴 Critical 이슈 (반드시 수정)

### C-001: Gradle Wrapper 파일 누락
- **위치**: `services/dividend-engine/`, `services/webhook-gateway/`
- **문제**: `gradlew`, `gradle/wrapper/` 파일이 없어 CI에서 빌드 불가
- **영향**: GitHub Actions CI 파이프라인 100% 실패
- **해결**: 각 서비스 디렉토리에서 `gradle wrapper` 실행하여 wrapper 생성

### C-002: DividendCalculationService에서 ISA 누적 배당 미추적
- **위치**: `DividendCalculationService.java:resolveTaxRate()`
- **문제**: ISA 비과세 한도 판단 시 "현재 배당 건"만 비교하고, 연간 누적 배당소득을 추적하지 않음
- **영향**: 200만원 이하 개별 배당이 여러 건 발생해도 항상 비과세 처리됨 (세금 계산 오류)
- **해결**: 연간 누적 배당소득 추적 테이블 추가, 또는 MVP에서는 명확한 TODO + 제한사항 문서화

### C-003: 단위 테스트 0건
- **위치**: 전체 백엔드 서비스
- **문제**: 테스트 코드가 하나도 없음. 특히 금융 계산 로직에 테스트 필수.
- **영향**: 세율 적용 오류를 잡을 수 없음, CI에서 테스트 단계가 무의미
- **해결**: 최소한 `DividendCalculationService` 테이블 드리븐 테스트 작성

---

## 🟡 Warning 이슈

### W-001: webhook-gateway에 DividendEngineClient 하드코딩 응답
- **위치**: `DividendEngineClient.java`
- **문제**: 모든 메서드가 하드코딩된 문자열 반환 (실제 API 호출 없음)
- **평가**: MVP 초기 단계에서 수용 가능하나, TODO 주석 보강 필요
- **제안**: 각 메서드에 `@Deprecated` 또는 명확한 MVP placeholder 표시

### W-002: admin-dashboard npm lockfile 미존재
- **위치**: `services/admin-dashboard/`
- **문제**: `package-lock.json`이 없어 CI에서 `npm ci` 실패
- **해결**: `npm install` 실행 후 lockfile 커밋

### W-003: go-patterns.md steering 파일 불필요
- **위치**: `.kiro/steering/go-patterns.md`
- **문제**: 프로젝트가 Java로 전환되었으나 Go 패턴 파일이 남아있음
- **해결**: 삭제 또는 `java-patterns.md`로 교체

### W-004: DB 스키마 init.sql에 테이블 생성문 없음
- **위치**: `infra/db/init.sql`
- **문제**: INSERT만 있고 CREATE TABLE이 없음. JPA ddl-auto=update에 의존하는데, 시드 데이터 INSERT가 JPA 이전에 실행되면 오류
- **해결**: JPA가 테이블 생성 후 별도 시드 스크립트 실행하도록 분리, 또는 CREATE TABLE 추가

### W-005: 환경변수 기본값에 실제 credentials 포함
- **위치**: `application.yml` (dividend-engine)
- **문제**: `DB_PASSWORD:postgres` 기본값이 있어 실수로 프로덕션에서 사용 가능
- **제안**: 기본값 제거하고 환경변수 미설정 시 시작 실패하도록 변경 (프로덕션 배포 시)

---

## ✅ 잘된 점
1. BigDecimal 사용으로 금액 계산 정확도 보장 (float 미사용) ✅
2. 세율을 `TaxRateConfig`로 외부화 (하드코딩 아님) ✅
3. 원 단위 이하 절사(Floor) 적용 ✅
4. 카카오 응답 포맷 헬퍼 메서드로 재사용성 확보 ✅
5. 서비스 분리 (MSA 구조) 적절 ✅
6. 면책 문구 포함 ✅

---

## 📋 Planner에게 전달할 개선 사항

이 내용을 인간이 확인 후 Planner Agent에게 전달하여 PRD를 보완합니다:

1. **ISA 누적 배당소득 추적 기능** — 현재 PRD에 누락. user_dividend_accumulation 테이블 필요
2. **단위 테스트 전략** — PRD NFR에 "테스트 커버리지 80%"를 명시하고 태스크로 분할
3. **실제 한투 API 연동 태스크** — 현재 하드코딩 응답만 존재. Phase 2 태스크로 명확히 분리
4. **기술 스택 문서 정합성** — Go 관련 파일 정리, Java 패턴 steering 추가
5. **admin-dashboard API 연동** — 백엔드 `/api/admin/stats` 엔드포인트 실제 구현 필요
