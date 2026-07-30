# 홍보지도 × InvestingBoard 백엔드

홍보 실행지도, 검수형 영상 렌더와 InvestingBoard 금융 포럼을 함께 제공하는 API 서버입니다.

## 서비스 구성
| 서비스 | 포트 | 설명 |
|--------|------|------|
| dividend-engine | 8080 | 기존 인증·포트폴리오 API |
| webhook-gateway | 8081 | 카카오 챗봇 스킬 서버 |
| notification | 8082 | 카카오 알림톡 발송 |
| news-service | 8083 | 홍보 분석·계획, 영상 렌더, InvestingBoard 뉴스·포럼 |

홍보 기능을 별도 JVM으로 추가하지 않고 기존 `news-service`에 통합해 EC2 메모리와 운영 포트를 늘리지 않습니다.

## 홍보지도 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/v1/promotion-sources` | URL·소개글·상품·매장·앱·콘텐츠를 홍보 브리프로 정규화 |
| POST | `/api/v1/promotion-plans` | 목표·고객·지역·예산으로 채널·비용·실행 행동 생성 |
| POST | `/api/content-videos/render` | 사람이 승인한 7장면 대본을 비동기 MP4로 렌더 |

홍보 분석은 현재 입력값과 공개 URL의 주소만 사용하며 외부 페이지 본문이나 파일을 자동 수집하지 않습니다.

## 로컬 실행
```bash
docker compose up -d
```

## 배포
`main` 반영 후 GitHub Actions의 `Deploy to EC2` 워크플로를 수동 실행합니다. EC2 인스턴스가
실행 중이고 `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`가 현재 인스턴스와 일치해야 합니다.

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
VIDEO_RENDER_ENABLED=false
VIDEO_RENDER_ACCESS_KEY=(프론트 VIDEO_RENDER_ACCESS_KEY와 동일)
PIXABAY_API_KEY=(선택, 없으면 자체 카드만 사용)
```
