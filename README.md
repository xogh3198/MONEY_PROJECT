# MoneyForum 백엔드

경제 포럼 & 배당 관리 서비스의 백엔드 API 서버.

## 서비스 구성
| 서비스 | 포트 | 설명 |
|--------|------|------|
| dividend-engine | 8080 | 배당 계산, 포트폴리오, 인증, 관리자 API |
| webhook-gateway | 8081 | 카카오 챗봇 스킬 서버 |
| notification | 8082 | 카카오 알림톡 발송 |
| news-service | 8083 | 뉴스 수집, 포럼, 시장지표 |

## 로컬 실행
```bash
docker compose up -d
```

## 배포
main 브랜치 push 시 GitHub Actions로 EC2(43.200.177.146)에 자동 배포됩니다.

## 필수 환경변수
```
DB_URL=jdbc:postgresql://postgres:5432/dividend_bot
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=(32자 이상)
KIS_APP_KEY=(한투 API)
KIS_APP_SECRET=(한투 API)
NAVER_CLIENT_ID=(네이버 뉴스 API)
NAVER_CLIENT_SECRET=(네이버 뉴스 API)
KAKAO_CLIENT_ID=(카카오 OAuth)
KAKAO_SENDER_KEY=(알림톡)
```
