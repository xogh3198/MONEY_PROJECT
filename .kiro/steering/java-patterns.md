---
inclusion: fileMatch
fileMatchPattern: "**/*.java"
---

## Java Spring Boot 패턴

### 프로젝트 구조 (서비스별)
```
services/{service-name}/
├── build.gradle.kts
├── src/main/java/com/dividendbot/{service}/
│   ├── {Service}Application.java     # @SpringBootApplication
│   ├── config/                       # @Configuration, Properties
│   ├── controller/                   # @RestController (입력 검증만)
│   ├── service/                      # @Service (비즈니스 로직)
│   ├── domain/
│   │   ├── entity/                   # @Entity (JPA)
│   │   └── repository/              # JpaRepository 인터페이스
│   └── dto/                          # Request/Response DTO
├── src/main/resources/
│   └── application.yml
└── src/test/java/                    # 테스트
```

### 필수 패턴
- 금액: `BigDecimal` 사용 (float/double 절대 금지)
- 반올림: `setScale(0, RoundingMode.FLOOR)` — 원 단위 이하 절사
- DI: 생성자 주입 (`@RequiredArgsConstructor`)
- 불변 엔티티: `@Builder` + `@NoArgsConstructor(access = PROTECTED)`
- 트랜잭션: `@Transactional(readOnly = true)` 기본, 쓰기 시 `@Transactional`
- 에러: 도메인 예외 정의 (`BusinessException` 계층)
- 로깅: `@Slf4j` (Lombok), 금융 데이터 마스킹
- 설정: `@ConfigurationProperties` 타입 세이프 바인딩
- 테스트: JUnit 5 + `@ParameterizedTest` 테이블 드리븐

### BigDecimal 사용 규칙
```java
// ✅ 올바른 사용
BigDecimal rate = new BigDecimal("0.154");
BigDecimal tax = amount.multiply(rate).setScale(0, RoundingMode.FLOOR);

// ❌ 절대 금지
double rate = 0.154;  // float/double 사용 금지
new BigDecimal(0.154); // double 생성자 금지 (정밀도 손실)
```

### 테스트 패턴
```java
@ParameterizedTest
@CsvSource({"361, 100, 36100", "1200, 50, 60000"})
void 배당금_계산(int perShare, int qty, int expected) {
    // given-when-then
}
```

### 헬스체크 (Spring Actuator)
- application.yml에 actuator 노출 설정 필수
- `/actuator/health` — liveness + readiness 자동 제공
