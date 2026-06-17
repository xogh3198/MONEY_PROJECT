# PRD: 경제 포럼 & 투자 인사이트 플랫폼 (서비스 피벗)

## 1. 개요

### 비전
주식 투자자를 위한 **경제 뉴스 포럼 + 시장 지표 예측 + 배당 관리 도구**를 
하나의 플랫폼에서 제공하는 종합 투자 정보 서비스.

### 전략
- **1차 목표**: 경제 포럼으로 초기 사용자(투자자 커뮤니티) 확보
- **2차 목표**: 시장 지표 시각화로 체류 시간 증가
- **3차 목표**: 배당금 관리(기존 기능)를 프리미엄/부가 기능으로 전환

### 서비스 구조 변경
```
[기존] 카카오 챗봇 중심 배당금 봇
         ↓ 피벗
[신규] 웹 플랫폼 (메인)
       ├── 📰 경제 포럼 (뉴스 큐레이션 + 커뮤니티) ← 초기 사용자 유입
       ├── 📊 시장 지표 대시보드 (환율, 주가, 금리 예측) ← 체류 시간
       ├── 💰 배당금 관리 (기존 기능) ← 부가 서비스
       └── 🔔 알림 설정 (카카오/텔레그램) ← 리텐션
```

---

## 2. 기능 요구사항 (FR)

### 핵심 기능 A: 경제 포럼

#### FR-A01: 경제 뉴스 자동 수집 & 큐레이션
- 주요 경제 뉴스 RSS/API 수집 (한경, 매경, 연합뉴스 경제)
- AI 기반 뉴스 요약 (핵심 3줄 요약)
- 카테고리 분류: 국내증시, 해외증시, 환율, 금리, 부동산, 암호화폐
- 관련 종목/지표 자동 태깅

#### FR-A02: 이슈 토론 게시판
- 뉴스 기사별 댓글/토론 기능
- 사용자 의견: 긍정(🟢) / 부정(🔴) / 중립(⚪) 투표
- 인기 이슈 랭킹 (조회수, 댓글수, 투표수 기반)
- 실시간 핫 이슈 표시

#### FR-A03: 사용자 프로필 & 활동
- 회원가입/로그인 (카카오 OAuth, 구글 OAuth)
- 활동 이력 (댓글, 투표, 북마크)
- 관심 종목/카테고리 설정
- 예측 적중률 표시 (추후)

### 핵심 기능 B: 시장 지표 대시보드

#### FR-B01: 실시간 경제 지표 표시
- 코스피/코스닥 지수
- 원/달러 환율
- 미국 S&P500, 나스닥
- 한국 기준금리
- 비트코인 가격

#### FR-B02: 예상 흐름도 (AI 분석)
- 뉴스 감성 분석 기반 시장 방향 예측 (상승/하락/보합)
- 과거 유사 패턴 비교
- 주요 이벤트 캘린더 (FOMC, 금통위, 실적 발표)
- **면책**: "AI 분석 결과이며 투자 조언이 아닙니다" 필수 표시

#### FR-B03: 차트 시각화
- 지표별 일봉/주봉/월봉 차트
- 뉴스 이벤트와 지표 변동 오버레이
- 이동평균선, 볼린저 밴드 등 기본 보조지표

### 부가 기능 C: 배당금 관리 (기존 기능 통합)

#### FR-C01: 배당금 캘린더 (기존)
- 웹 UI로 월별 배당 캘린더 표시
- 포트폴리오 등록/관리
- ISA/IRP 절세 최적화

#### FR-C02: 알림 설정
- 배당락일 알림
- 관심 종목 뉴스 알림
- 시장 급변 알림 (급등/급락)

---

## 3. 비기능 요구사항 (NFR)

| 항목 | 기준 |
|------|------|
| 페이지 로딩 | < 2초 (LCP) |
| 동시 접속 | 500명 (MVP) |
| 뉴스 수집 주기 | 15분 간격 |
| 시장 지표 갱신 | 1분 간격 (장중) |
| SEO | SSR/SSG 적용 (초기 사용자 유입) |
| 모바일 반응형 | 필수 |

---

## 4. 기술 아키텍처 변경

```
[Frontend - Next.js 14 (App Router)]
├── / (메인: 핫 이슈 + 지표 요약)
├── /forum (경제 포럼: 뉴스 + 토론)
├── /market (시장 지표 대시보드 + 차트)
├── /dividend (배당금 관리 - 기존 기능)
└── /mypage (프로필, 알림 설정)

[Backend - Spring Boot (기존 유지 + 확장)]
├── dividend-engine (기존 유지)
├── webhook-gateway (기존 유지)
├── notification (기존 유지)
├── news-service (신규: 뉴스 수집/요약/분류)
├── market-indicator (신규: 시장 지표 수집/예측)
└── forum-service (신규: 게시판/댓글/투표)

[인프라]
├── PostgreSQL (기존)
├── Redis (캐시: 실시간 지표, 세션)
└── Elasticsearch (뉴스 검색 - Phase 2)
```

---

## 5. 데이터 모델 (신규)

### news_articles
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| title | VARCHAR(200) | 기사 제목 |
| summary | TEXT | AI 3줄 요약 |
| source_url | TEXT | 원문 URL |
| source_name | VARCHAR(50) | 출처 (한경, 매경 등) |
| category | ENUM | DOMESTIC, OVERSEAS, FOREX, RATE, CRYPTO |
| sentiment | ENUM | POSITIVE, NEGATIVE, NEUTRAL |
| related_stocks | VARCHAR[] | 관련 종목코드 배열 |
| view_count | INTEGER | 조회수 |
| published_at | TIMESTAMP | 기사 발행일 |
| created_at | TIMESTAMP | 수집일 |

### forum_comments
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| article_id | UUID | FK → news_articles |
| user_id | UUID | FK → users |
| content | TEXT | 댓글 내용 |
| vote | ENUM | POSITIVE, NEGATIVE, NEUTRAL |
| created_at | TIMESTAMP | 작성일 |

### market_indicators
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | UUID | PK |
| indicator_type | ENUM | KOSPI, KOSDAQ, USD_KRW, SP500, BTC |
| value | DECIMAL | 현재값 |
| change_rate | DECIMAL | 변동률 (%) |
| prediction | ENUM | UP, DOWN, NEUTRAL |
| recorded_at | TIMESTAMP | 기록 시점 |

---

## 6. 태스크 분할 (Phase 5: 피벗)

| ID | 태스크 | 서비스 | 복잡도 | 선행 |
|----|--------|--------|--------|------|
| PV-001 | Next.js 프론트엔드 프로젝트 세팅 (App Router, SSR) | frontend | M | - |
| PV-002 | 메인 페이지 레이아웃 + 네비게이션 | frontend | S | PV-001 |
| PV-003 | news-service: 뉴스 수집 크롤러 (RSS) | news-service | M | - |
| PV-004 | news-service: 뉴스 API (목록, 상세, 카테고리) | news-service | S | PV-003 |
| PV-005 | forum-service: 댓글/투표 CRUD | forum-service | M | - |
| PV-006 | 포럼 페이지 UI (뉴스 목록 + 댓글 + 투표) | frontend | M | PV-004, PV-005 |
| PV-007 | market-indicator: 시장 지표 수집 (환율, 주가) | market-indicator | M | - |
| PV-008 | 시장 지표 대시보드 UI (차트, 예측 방향) | frontend | M | PV-007 |
| PV-009 | 사용자 인증 (카카오 OAuth + JWT) | auth | M | - |
| PV-010 | 배당금 관리 페이지 (기존 기능 웹 UI 이식) | frontend | S | PV-001 |

### MVP 범위 (이번 사이클)
**PV-001 ~ PV-006**: 프론트엔드 + 뉴스 서비스 + 포럼 기능
나머지는 다음 사이클에서 진행.
