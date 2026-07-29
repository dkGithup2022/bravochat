# 브라보 챗 — FE 핸드오프 (API 스펙 & 주요 시나리오)

> BE 기준 코드에서 추출한 계약. 실제 필드명/상태코드는 이 문서를 정본으로 사용.
> 스펙 변경 시 이 문서도 함께 갱신.

---

## 0. 공통

| 항목 | 값 |
|---|---|
| Base URL (local) | `http://localhost:8080` |
| 인증 방식 | Bearer 토큰 (세션 키). 로그인 응답의 `Authorization` 헤더로 발급 |
| 세션 TTL | 발급 시점 기준 **절대 만료 7일** (요청마다 슬라이딩 갱신 아님) |
| 요청/응답 포맷 | `application/json` (로그인/로그아웃/에러 바디) |
| API 문서 | Swagger UI: `http://localhost:8080/swagger-ui.html` |

### 인증 헤더 규칙
- 로그인/로그아웃 제외한 **모든 엔드포인트는 인증 필수**.
- 요청 시 헤더: `Authorization: Bearer {sessionKey}`
- 헤더 없음 / `Bearer ` 접두어 누락 / 세션 없음 / 만료 → **401** (`InvalidSessionException`)
  → FE 처리: 세션 만료로 간주하고 로그인 화면으로 유도.

### ⚠️ FE 착수 전 BE에 확인 필요 (현재 미구현/미설정)
- **CORS 미설정**: 현재 코드에 CORS 매핑 없음. FE dev 서버(별도 origin)에서 호출하려면 BE에 CORS 허용 origin 추가 필요. **착수 전 요청할 것.**
- 스트리밍 없음: `/chat/turns`는 **동기 요청/응답**. 토큰 스트리밍(SSE/WebSocket) 아님 — 응답이 완성될 때까지 기다렸다가 한 번에 반환.

---

## 1. 에러 응답 (공통 포맷)

모든 에러는 아래 JSON 형태로 반환:

```json
{
  "status": 400,
  "error": "ValidationFailed",
  "message": "message: 사용자 입력 텍스트는 필수입니다"
}
```

| status | error (대표) | 발생 상황 | FE 처리 가이드 |
|---|---|---|---|
| 400 | `ValidationFailed` | 요청 바디 검증 실패 (빈 message 등) | 입력 필드 에러 표시 |
| 400 | `MalformedBody` / `MissingParameter` / `TypeMismatch` | 잘못된 JSON / 파라미터 누락·형식오류 | 개발 단계 버그, 토스트 |
| 400 | `InvalidRecentTurnSizeException` | `size`가 1~20 범위 밖 | size 보정 후 재요청 |
| 401 | `LoginFailedException` | 로그인 실패 (아이디/비번 불일치) | "아이디 또는 비밀번호가 올바르지 않습니다" |
| 401 | `InvalidSessionException` | 세션 없음/만료/형식오류 | 로그인 화면으로 |
| 404 | `ScheduleNotFoundException` | 없는 일정 또는 타 유저 일정 접근 (구분 없음) | "일정을 찾을 수 없습니다" + 목록 새로고침 |
| 500 | `LlmExecutionException` | LLM 호출/처리 실패 | "응답 생성에 실패했습니다" 재시도 유도 |
| 500 | `InternalServerError` | 그 외 서버 오류 | 일반 오류 토스트 |

> 보안: 로그인 실패는 계정 존재 여부를 구분하지 않음 (메시지 동일).

---

## 2. 엔드포인트

### 2-1. 로그인 — `POST /auth/login`
인증 불필요한 **유일한** 엔드포인트.

**Request**
```json
{ "username": "tester", "password": "password1234" }
```
- `username`: 필수, non-blank
- `password`: 필수, non-blank

**Response** — `204 No Content`
- 바디 없음. 세션 키는 **응답 헤더**로 발급:
  ```
  Authorization: Bearer 3f9c1a...-uuid
  ```
- FE: 이 헤더에서 토큰을 추출해 저장(메모리/스토리지)하고 이후 요청에 그대로 붙임.

**에러**: 401 `LoginFailedException` (인증 실패), 400 (필드 누락)

---

### 2-2. 로그아웃 — `DELETE /auth/session`
인증 필요.

**Request**: 바디 없음. 헤더 `Authorization: Bearer {key}`
**Response** — `204 No Content` (멱등 — 이미 만료된 세션이어도 성공)
**에러**: 401 (세션 무효)

---

### 2-3. 대화 요청 — `POST /chat/turns`
인증 필요. 메시지 전송 → **최종 응답을 동기로 반환**. 세션의 userId로 처리, 최근 20턴을 컨텍스트로 사용.

**Request**
```json
{ "message": "안녕하세요" }
```
- `message`: 필수, non-blank, **최대 4000자**

**Response** — `200 OK`
```json
{
  "turnId": 42,
  "message": "안녕하세요! 무엇을 도와드릴까요?",
  "createdAt": "2026-07-24T05:12:30.123Z"
}
```
| 필드 | 타입 | 설명 |
|---|---|---|
| `turnId` | number | 생성된 턴 ID |
| `message` | string | 어시스턴트 최종 응답 텍스트 |
| `createdAt` | string(ISO-8601, UTC) | 턴 생성 시각 |

**에러**: 400 (빈/초과 message), 401 (세션), 500 `LlmExecutionException` (생성 실패)

> ⚠️ 응답 지연: LLM 처리 시간만큼 걸림. FE는 요청 중 로딩 상태(타이핑 인디케이터 등)를 반드시 표시하고, 넉넉한 타임아웃을 둘 것.

---

### 2-4. 최근 대화 조회 — `GET /chat/turns/recent`
인증 필요. **완료된 턴의 사용자 입력 + 최종 응답만** 반환. 정렬: **오래된→최신**.

**Query Param**
- `size` (optional, default `20`): 조회 개수. **허용 범위 1~20**, 벗어나면 400.

**Response** — `200 OK`
```json
{
  "turns": [
    {
      "turnId": 40,
      "userMessage": "오늘 날씨 어때?",
      "assistantMessage": "제가 실시간 날씨는...",
      "createdAt": "2026-07-24T04:50:10.000Z"
    },
    {
      "turnId": 41,
      "userMessage": "고마워",
      "assistantMessage": "천만에요!",
      "createdAt": "2026-07-24T04:51:02.000Z"
    }
  ]
}
```
| 필드 | 타입 | 설명 |
|---|---|---|
| `turns[]` | array | 오래된→최신 순 |
| `turns[].turnId` | number | 턴 ID |
| `turns[].userMessage` | string | 사용자 입력 |
| `turns[].assistantMessage` | string | 어시스턴트 최종 응답 |
| `turns[].createdAt` | string(ISO-8601) | 턴 생성 시각 |

**에러**: 400 (size 범위 밖), 401 (세션)

> 진행 중(미완료) 턴은 포함되지 않음. 페이지네이션 없음 — 최근 N개만.

---

### 2-5. 일정 기간 조회 — `GET /schedules`
인증 필요. 기간 내 일정을 **최신순(scheduled_at 역순)** 으로 반환.

**Query Param**
- `from` (optional, `YYYY-MM-DD`): 시작일(포함). 생략 시 **오늘(KST)**
- `to` (optional, `YYYY-MM-DD`): 종료일(**포함**). 생략 시 from+6일 (총 7일)
- `size` (optional, default `20`): 최대 건수. 1~100 밖 값은 서버가 범위로 보정 (에러 아님)

**Response** — `200 OK`
```json
{
  "schedules": [
    {
      "scheduleId": 3,
      "title": "돌돌이 미팅",
      "content": null,
      "scheduleType": "ETC",
      "scheduledAt": "2026-07-30T08:00:00Z",
      "done": false
    },
    {
      "scheduleId": 1,
      "title": "강남 미팅",
      "content": "강남역",
      "scheduleType": "WORK",
      "scheduledAt": "2026-07-30T06:00:00Z",
      "done": false
    }
  ]
}
```
| 필드 | 타입 | 설명 |
|---|---|---|
| `schedules[]` | array | **최신순** (scheduled_at 내림차순) |
| `schedules[].scheduleId` | number | 일정 ID — **변경(PATCH) 시 새 ID로 바뀜** (아래 2-7 참고) |
| `schedules[].title` | string | 제목 (최대 200자) |
| `schedules[].content` | string \| null | 상세 |
| `schedules[].scheduleType` | string | `HEALTH` \| `PERSONAL` \| `WORK` \| `ETC` |
| `schedules[].scheduledAt` | string(ISO-8601 UTC) | 일정 시각 — **KST 변환은 FE 책임** |
| `schedules[].done` | boolean | 완료 여부 |

**에러**: 400 (from/to 날짜 형식 오류), 401 (세션)

> 챗봇으로 만든 일정과 이 API로 만든 일정이 **같은 데이터**다 — 챗에서 등록한 일정이 여기 조회에 그대로 나온다.

---

### 2-6. 일정 등록 — `POST /schedules`
인증 필요.

**Request**
```json
{
  "title": "강남 미팅",
  "content": "강남역 2번 출구",
  "scheduleType": "WORK",
  "scheduledAt": "2026-07-30T06:00:00Z"
}
```
- `title`: 필수, non-blank, 최대 200자
- `content`: optional
- `scheduleType`: optional. `HEALTH|PERSONAL|WORK|ETC` 외 값·생략은 `ETC`로 흡수 (에러 아님)
- `scheduledAt`: 필수, ISO-8601 UTC instant

**Response** — `201 Created`: 생성된 일정 객체 (2-5와 동일 형태)

**에러**: 400 (`ValidationFailed` — title 누락/200자 초과, scheduledAt 누락), 401 (세션)

---

### 2-7. 일정 변경 — `PATCH /schedules/{scheduleId}`
인증 필요. **보낸 필드만 변경** (생략/null은 기존 값 유지).

**⚠️ 교체 방식**: 변경은 내부적으로 "새 row 추가 + 기존 row 삭제"로 처리된다.
**응답의 `scheduleId`가 요청한 ID와 달라진다** — FE는 응답의 새 ID로 로컬 상태를 갱신할 것.

**Request** (모든 필드 optional — 최소 1개 권장)
```json
{ "scheduledAt": "2026-07-30T11:00:00Z" }
```

**Response** — `200 OK`: 변경 후 일정 객체 (**새 `scheduleId`**)

**에러**: 404 `ScheduleNotFoundException` (없는 일정 **또는 타 유저 일정** — 구분 없음), 400, 401

---

### 2-8. 일정 삭제 — `DELETE /schedules/{scheduleId}`
인증 필요.

**Response** — `204 No Content`

**에러**: 404 `ScheduleNotFoundException` (없는 일정 또는 타 유저 일정), 401

> 소유권 정책: 타 유저의 일정은 403이 아니라 **404** — 존재 자체를 노출하지 않는다.

---

## 3. 주요 시나리오

### S1. 최초 진입 → 로그인
1. FE 앱 진입, 저장된 토큰 없음.
2. `POST /auth/login` (username/password)
3. 200(204) → `Authorization` 헤더에서 토큰 추출·저장 → 채팅 화면으로.
4. 실패(401) → 에러 메시지, 로그인 화면 유지.

### S2. 채팅 화면 초기 로드 (대화 이력 복원)
1. 토큰 보유 상태로 채팅 화면 진입.
2. `GET /chat/turns/recent?size=20`
3. 응답 `turns`를 **오래된→최신** 순으로 말풍선 렌더 (userMessage → assistantMessage 쌍).
4. 401 → 세션 만료, 로그인 화면으로.

### S3. 메시지 전송 (핵심 루프)
1. 사용자가 입력 후 전송.
2. FE: 사용자 말풍선 즉시 낙관적 렌더 + 로딩 인디케이터 표시.
3. `POST /chat/turns` `{ message }`
4. 200 → `message`를 어시스턴트 말풍선으로 렌더, 로딩 해제. (`turnId`/`createdAt` 보관)
5. 400 → 입력 오류 표시(로딩 해제, 사용자 말풍선 롤백 또는 재시도 유도).
6. 500 `LlmExecutionException` → "응답 생성 실패" + 재시도 버튼.
7. 401 → 세션 만료 처리.

### S4. 세션 만료 처리 (횡단 관심사)
- **모든** 인증 필요 호출에서 401 수신 시: 토큰 폐기 → 로그인 화면 리다이렉트.
- 세션은 발급 후 7일 절대 만료. (요청해도 연장되지 않음 — 만료 시 재로그인)

### S5. 로그아웃
1. 사용자가 로그아웃.
2. `DELETE /auth/session`
3. 204(멱등) → 토큰 폐기 → 로그인 화면.

---

### S6. 일정 화면 (조회·수정)
1. 일정 탭 진입 → `GET /schedules` (기본: 오늘부터 7일, 최신순) → 목록 렌더.
2. 등록: `POST /schedules` → 201 응답 객체를 목록에 반영.
3. 수정: `PATCH /schedules/{id}` → **응답의 새 `scheduleId`로 교체** (기존 id는 죽은 참조).
4. 삭제: `DELETE /schedules/{id}` → 204 → 목록에서 제거.
5. 404 수신 시(다른 기기에서 수정/삭제된 경우 등) → 목록 새로고침으로 동기화.
6. 챗봇("내일 3시 회의 잡아줘")으로 만든 일정도 같은 데이터 — 챗 사용 후 일정 화면 복귀 시 재조회 권장.

## 4. 로컬 개발용 시드 계정
- username: `tester`
- password: `password1234`

---

## 5. 미결정 / 향후 확장 (FE와 합의 필요)
- **스트리밍 응답**: 현재 없음. 실시간 타이핑 UX 원하면 BE에 SSE/WebSocket 논의 필요.
- **대화 세션 분리**: 현재 "대화방" 개념 없이 유저 단위 단일 타임라인. 멀티 스레드/방 필요 시 별도 설계.
- **페이지네이션 / 무한 스크롤**: recent는 최대 20개 고정. 과거 이력 더보기 미지원.
- **툴 사용/리치 응답**: 응답은 현재 plain text 하나. 툴콜·카드·이미지 등 구조화 응답은 미정.
- **CORS 허용 origin**: FE dev origin 확정되면 BE 반영.
