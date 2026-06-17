---
inclusion: fileMatch
fileMatchPattern: "**/kakao/**,**/notification/**,**/webhook**"
---

## 카카오톡 챗봇 & 알림톡 연동 가이드

### 카카오 i 오픈빌더 스킬 서버

#### 요청 수신 (POST /kakao/skill)
```json
{
  "intent": {"name": "배당금조회"},
  "userRequest": {
    "utterance": "이번 달 배당금 알려줘",
    "user": {"id": "abc123"}
  },
  "action": {
    "params": {"stockCode": "005930"}
  }
}
```

#### 응답 포맷
```json
{
  "version": "2.0",
  "template": {
    "outputs": [
      {"simpleText": {"text": "메시지 내용"}},
      {"basicCard": {"title": "제목", "description": "내용", "buttons": [...]}}
    ],
    "quickReplies": [
      {"label": "포트폴리오 보기", "action": "message", "messageText": "포트폴리오"}
    ]
  }
}
```

#### 응답 시간 제한
- 스킬 서버 응답: **5초 이내** (초과 시 타임아웃)
- 긴 작업은 즉시 "처리 중" 응답 후 알림톡으로 결과 전송

### 지원 응답 타입
| 타입 | 용도 | 제한 |
|------|------|------|
| SimpleText | 단순 텍스트 응답 | 1,000자 |
| BasicCard | 이미지 + 제목 + 설명 + 버튼 | 버튼 3개 |
| ListCard | 목록형 (배당 달력 등) | 항목 5개 |
| Carousel | 카드 슬라이드 | 카드 10개 |

### 카카오 알림톡 발송

#### 필수 조건
- 카카오 비즈니스 채널 개설 완료
- 사업자 등록 (개인사업자 가능)
- 알림톡 템플릿 사전 승인 (1~3 영업일 소요)
- 수신자 전화번호 기반 발송 (카카오 계정 매칭)

#### 템플릿 예시
```
[배당락일 알림]
#{고객명}님, 보유 종목 배당락일 안내입니다.

종목: #{종목명}
배당락일: #{배당락일}
보유수량: #{보유수량}주
예상 배당금: #{예상배당금}원 (세후)

※ 본 정보는 투자 조언이 아닙니다.
```

#### 발송 규칙
- 발송 시간: 08:00~21:00 KST (야간 발송 금지)
- 수신 동의 확인 필수 (정보통신망법)
- 발송 실패 시 SMS 대체 발송 옵션
- 일일 발송 한도: 1,000건/채널 (초과 시 사전 협의)

### Intent 설계 (사용자 발화 매핑)
| 발화 예시 | Intent | 처리 서비스 |
|-----------|--------|------------|
| "포트폴리오 등록" | portfolio.register | user-account |
| "삼성전자 100주 추가" | portfolio.add | user-account |
| "이번 달 배당금" | dividend.monthly | dividend-engine |
| "다음 배당락일" | dividend.exdate | dividend-engine |
| "ISA 한도 확인" | account.isa_limit | user-account |
| "절세 전략" | strategy.tax | dividend-engine |
