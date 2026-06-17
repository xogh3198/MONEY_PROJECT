---
inclusion: fileMatch
fileMatchPattern: "**/market-data/**,**/kis/**"
---

## 한국투자증권 OpenAPI 연동 가이드

### 인증 (OAuth 2.0)
- 엔드포인트: POST /oauth2/tokenP
- 요청: { "grant_type": "client_credentials", "appkey": "...", "appsecret": "..." }
- 응답: { "access_token": "...", "token_type": "Bearer", "expires_in": 86400 }
- 유효기간: 24시간 → 만료 전 자동 재발급 로직 필수
- 환경 분리: 실전투자(prod) / 모의투자(paper) URL 다름

### Rate Limit (절대 준수)
- **초당 20건** 제한 (초과 시 429 응답)
- Token Bucket 패턴으로 호출 속도 제어
- 제한 초과 시: 1초 대기 후 재시도 (최대 3회, 지수 백오프)
- 일일 호출량 모니터링 → 비용 알림 설정

### 필수 헤더
```
authorization: Bearer {access_token}
appkey: {APP_KEY}
appsecret: {APP_SECRET}
tr_id: {거래ID}  # API별 고유값
custtype: "P"    # 개인
```

### 주요 API 목록
| API | tr_id | 용도 | 호출 주기 |
|-----|-------|------|-----------|
| 국내주식 현재가 | FHKST01010100 | 포트폴리오 평가액 | 사용자 요청 시 |
| 국내주식 배당금 | HHKDB669108C0 | 종목별 배당 정보 | 일 1회 (02:00) |
| 국내주식 일봉 | FHKST01010400 | 배당락일 전후 주가 | 필요 시 |
| 해외주식 현재가 (Phase 3) | HHDFS00000300 | 미국주식 가격 | 사용자 요청 시 |

### 데이터 수집 파이프라인
```
[매일 02:00 KST]
1. Token 유효성 확인 → 만료 시 재발급
2. KOSPI/KOSDAQ 배당 종목 리스트 조회
3. 종목별 배당 정보 수집 (rate limit 준수: 20건/초)
   - 주당배당금
   - 배당수익률
   - 배당기준일
   - 배당락일  
   - 배당지급예정일
4. MinIO에 원본 JSON 저장 (감사 추적용)
   - 경로: market-data/{date}/{stock_code}.json
5. PostgreSQL에 정제 데이터 upsert
6. Kafka topic 발행: market.dividend.updated
```

### 에러 처리
| HTTP 상태 | 의미 | 대응 |
|-----------|------|------|
| 200 | 성공 | 정상 처리 |
| 401 | 토큰 만료 | 토큰 재발급 후 재시도 |
| 429 | Rate Limit 초과 | 1초 대기 후 재시도 (최대 3회) |
| 500 | 서버 오류 | 5초 대기 후 재시도, 3회 실패 시 이전 캐시 사용 |

### 코드 구현 시 주의사항
- 모든 API 응답은 원본 JSON으로 MinIO에 보존 (디버깅/감사용)
- 영업일 판단: 한국거래소 휴장일 캘린더 참조
- 배당 정보 정정 가능성 고려 → upsert로 덮어쓰기
- 외부 API 장애 시 서비스 전체 장애 방지 → Circuit Breaker
