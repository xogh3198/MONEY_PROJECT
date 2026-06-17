# 💰 배당금 캘린더 & 절세 최적화 봇

카카오톡 챗봇 기반 배당금 관리 서비스입니다.

## 주요 기능
- 📊 월별 배당금 캘린더 조회
- 🔔 배당락일 사전 알림 (D-3, D-1)
- 💵 ISA/IRP 절세 최적화 한도 추적
- 📱 카카오톡 챗봇 + 알림톡 연동

## 기술 스택
- **Backend**: Java 17 + Spring Boot 3.3
- **Frontend**: React 18 + TypeScript + Vite
- **Database**: PostgreSQL 16
- **Infra**: Docker Compose → K3s + ArgoCD
- **CI/CD**: GitHub Actions

## 서비스 구성
| 서비스 | 포트 | 설명 |
|--------|------|------|
| dividend-engine | 8080 | 배당금 계산 & 포트폴리오 관리 |
| webhook-gateway | 8081 | 카카오 챗봇 스킬 서버 |
| admin-dashboard | 3000 | 관리자 대시보드 (React) |

## 로컬 실행
```bash
docker-compose up -d
```

## 프로젝트 구조
```
├── services/
│   ├── dividend-engine/     # 배당금 계산 엔진 (Spring Boot)
│   ├── webhook-gateway/     # 카카오 챗봇 게이트웨이 (Spring Boot)
│   └── admin-dashboard/     # 관리자 대시보드 (React)
├── .kiro/                   # 하네스 엔지니어링 (AI 가이드라인)
├── config/                  # 세율 등 외부 설정
├── infra/                   # DB 스키마, Terraform, Helm
└── .github/workflows/       # CI/CD 파이프라인
```

---
※ 본 서비스는 투자 조언을 제공하지 않으며, 정보 제공 목적으로만 운영됩니다.
