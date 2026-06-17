# PR 리뷰 — Phase 7

## 요약
- **변경**: 12개 파일, +266/-10 라인
- **Critical**: 0건 ✅
- **Warning**: 1건
- **판정**: ✅ Approve

## ✅ 체크

| 항목 | 결과 |
|------|------|
| JWT 서명 (HMAC-SHA) | ✅ jjwt 0.12.6 |
| Secret Key 환경변수 | ✅ ${JWT_SECRET} |
| Token 만료 설정 | ✅ 24시간 |
| Dockerfile multi-stage | ✅ (build→runtime 분리) |
| Docker Compose healthcheck | ✅ (postgres ready 대기) |
| API 클라이언트 모듈 분리 | ✅ (lib/api.ts) |
| forum 페이지 API fallback | ✅ (실패 시 Mock 유지) |
| CORS 설정 | ✅ |

## 🟡 Warning

### W-001: JWT Secret 기본값이 코드에 존재
- **위치**: `application.yml`, `JwtProvider.java`
- **판단**: 개발 편의용 기본값. 프로덕션 배포 시 반드시 환경변수로 오버라이드 필요.
- **조치**: README에 프로덕션 배포 시 필수 환경변수 명시 (이미 Docker에서 설정됨)

## ✅ Approve — 자동 머지
