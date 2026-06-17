---
inclusion: always
---

## MoneyForum 백엔드

### 개요
경제 포럼 + 배당 관리 서비스의 백엔드 API 서버.
Java Spring Boot 기반 멀티 모듈 MSA 구조.

### 기술 스택
- Java 17 + Spring Boot 3.3
- Spring Data JPA + PostgreSQL 16
- Spring WebFlux (WebClient — 외부 API 호출용)
- Gradle (Kotlin DSL)
- Docker + Docker Compose
- SpringDoc/Swagger (API 문서)

### 서비스 구성
| 서비스 | 포트 | 역할 |
|--------|------|------|
| dividend-engine | 8080 | 배당 계산, 포트폴리오, 인증, 관리자 API |
| webhook-gateway | 8081 | 카카오 챗봇 스킬 서버 |
| notification | 8082 | 카카오 알림톡 발송 |
| news-service | 8083 | 뉴스 수집/포럼/댓글/투표/시장지표 |

### 배포
- EC2: 43.200.177.146
- Docker Compose로 전체 실행
- GitHub Actions로 자동 배포
- 레포: xogh3198/MONEY_PROJECT

### 환경변수 (필수)
- DB_URL, DB_USERNAME, DB_PASSWORD
- JWT_SECRET
- KIS_APP_KEY, KIS_APP_SECRET
- NAVER_CLIENT_ID, NAVER_CLIENT_SECRET
- KAKAO_CLIENT_ID, KAKAO_SENDER_KEY
