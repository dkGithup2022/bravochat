# Backend API 계약 (be)

**정본: [`specs/api-handoff.md`](../specs/api-handoff.md)** — 필드명/상태코드/시나리오는 그 문서를 따른다. 아래는 FE 설계에 직접 영향 주는 요약·함의.

## 엔드포인트 요약
| 메서드 | 경로 | 인증 | 성공 | 비고 |
|--------|------|------|------|------|
| POST | `/auth/login` | ❌ | 204 + `Authorization: Bearer {key}` 헤더 | 토큰 발급(헤더에서 추출) |
| DELETE | `/auth/session` | ✅ | 204 (멱등) | 로그아웃 |
| POST | `/chat/turns` | ✅ | 200 `{turnId, message, createdAt}` | 동기, LLM 지연 있음 |
| GET | `/chat/turns/recent?size=20` | ✅ | 200 `{turns:[{turnId,userMessage,assistantMessage,createdAt}]}` | 오래된→최신, size 1~20 |

- Base URL: `http://localhost:8080` · 인증: `Authorization: Bearer {sessionKey}` · 세션 TTL **7일 절대 만료**(슬라이딩 아님)
- 시드 계정: `tester` / `password1234`

## 에러 공통 포맷
`{ "status": number, "error": string, "message": string }`
- 400 `ValidationFailed` / `InvalidRecentTurnSizeException`(size 범위)
- 401 `LoginFailedException`(로그인 실패) / `InvalidSessionException`(세션 없음·만료)
- 500 `LlmExecutionException`(생성 실패) / `InternalServerError`

## FE 설계 함의 (⚠️ 반드시 반영)
1. **응답은 plain text `message` 하나뿐.** BE는 schedule/options/card 같은 **구조화 응답을 주지 않음**(정본 §5 미정).
   → 시안의 `ScheduleCard`·`OptionChips`는 **FE 목/자리표시자 전용**. 실 BE 연동 시엔 bot 텍스트 버블만 렌더. Message union은 유지하되 be→Message 매핑은 user/bot 두 타입만.
2. **스트리밍 없음** — 동기 요청/응답. 전송 중 타이핑 인디케이터 + 넉넉한 타임아웃. 낙관적 유저 버블 즉시 렌더(S3).
3. **401 = 횡단 관심사** — 모든 인증 호출에서 401 시 토큰 폐기 → 로그인 화면. fetch 래퍼에서 일괄 처리.
4. **CORS 미설정(블로커)** — dev 서버(별도 origin)에서 실 BE 직접 호출 불가. 목 먼저 전략과 정합. 실 BE 스위치 전 ①BE CORS 허용 or ②Next route handler 프록시(same-origin) 중 택1. **프록시 방식이면 CORS 회피 가능** → 권장.
5. **turnId/createdAt 보관** — 전송 응답의 메타 저장(향후 정렬/키).
6. **recent size 고정 20, 페이지네이션 없음** — 과거 더보기 UI 미지원.

## 목킹 규칙 (M3)
Next route handler로 위 계약을 **에러 포맷까지 동일하게** 목킹. 200 응답 전 인위적 지연(예: 800~1500ms)으로 타이핑 UX 검증.
