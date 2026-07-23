# Chat API Specification

## 1. 로그인

### `POST /auth/login`

인증이 필요하지 않은 유일한 API입니다.

#### Request Body

```json
{
  "username": "string",
  "password": "string"
}
```

#### Success Response

```http
204 No Content
Authorization: Bearer {sessionKey}
```

#### 실패

| 상태 | 조건 |
|---|---|
| `400 Bad Request` | 필수 값 누락 또는 형식 오류 |
| `401 Unauthorized` | 계정이 없거나 비밀번호가 일치하지 않음 |

---

## 2. 인증 방식

로그인 이후 모든 API 요청에 다음 헤더를 포함합니다.

```http
Authorization: Bearer {sessionKey}
```

서버는 다음 순서로 인증을 처리합니다.

1. 세션 키 조회
2. 세션 존재 여부 확인
3. 세션 만료 여부 확인
4. 세션에서 `userId` 추출
5. 해당 사용자 기준으로 요청 처리

클라이언트는 요청 Body에 `userId`를 전달하지 않습니다.

#### 인증 실패

| 상태 | 조건 |
|---|---|
| `401 Unauthorized` | `Authorization` 헤더 없음 |
| `401 Unauthorized` | Bearer 형식 오류 |
| `401 Unauthorized` | 존재하지 않는 세션 |
| `401 Unauthorized` | 만료된 세션 |

---

## 3. 대화 요청

### `POST /chat/turns`

사용자 메시지를 전송하고 최종 응답을 반환합니다.

#### Request Header

```http
Authorization: Bearer {sessionKey}
```

#### Request Body

```json
{
  "message": "사용자 입력 텍스트"
}
```

#### Success Response

```http
200 OK
```

```json
{
  "turnId": 102,
  "message": "LLM 최종 응답",
  "createdAt": "2026-07-23T20:10:32"
}
```

#### 처리 기준

- 인증된 세션의 `userId`를 기준으로 대화를 처리합니다.
- 사용자 입력과 최종 LLM 응답을 하나의 Turn으로 저장합니다.
- 도구 호출이 발생해도 클라이언트에는 최종 응답만 반환합니다.
- 현재는 스트리밍 없이 요청 완료 후 전체 결과를 반환합니다.

#### 실패

| 상태 | 조건 |
|---|---|
| `400 Bad Request` | 메시지가 비어 있거나 최대 길이를 초과함 |
| `401 Unauthorized` | 세션 인증 실패 |
| `500 Internal Server Error` | LLM 또는 도구 호출 처리 실패 |

---

## 4. 최근 대화 조회

### `GET /chat/turns/recent`

현재 로그인한 사용자의 최근 대화 최대 20개를 조회합니다.

#### Request Header

```http
Authorization: Bearer {sessionKey}
```

#### Success Response

```http
200 OK
```

```json
{
  "turns": [
    {
      "turnId": 81,
      "userMessage": "안녕하세요.",
      "assistantMessage": "안녕하세요. 무엇을 도와드릴까요?",
      "createdAt": "2026-07-23T19:50:12"
    },
    {
      "turnId": 82,
      "userMessage": "오늘 일정을 정리해줘.",
      "assistantMessage": "오늘 일정은 다음과 같습니다.",
      "createdAt": "2026-07-23T19:51:30"
    }
  ]
}
```

#### 처리 기준

- 인증된 세션의 `userId`를 기준으로 조회합니다.
- 가장 최근에 생성된 Turn 최대 20개를 조회합니다.
- 화면 출력이 자연스럽도록 응답은 오래된 대화부터 최신 대화 순으로 정렬합니다.
- 저장된 대화가 20개 미만이면 존재하는 대화만 반환합니다.
- 대화가 없으면 빈 배열을 반환합니다.

```json
{
  "turns": []
}
```

#### 실패

| 상태 | 조건 |
|---|---|
| `401 Unauthorized` | 세션 인증 실패 |
| `500 Internal Server Error` | 대화 조회 실패 |

#### TODO

- 현재는 최근 20개 고정 조회로 구현합니다.
- 이후 과거 대화 추가 로딩을 위해 커서 기반 페이지네이션을 적용합니다.
- 커서는 `turnId` 또는 `(createdAt, turnId)` 조합을 검토합니다.
- API 예시:

```http
GET /chat/turns/recent?cursor={cursor}&size=20
```

---

## 5. 로그아웃

### `DELETE /auth/session`

현재 요청에 사용된 세션을 만료하거나 삭제합니다.

#### Request Header

```http
Authorization: Bearer {sessionKey}
```

#### Success Response

```http
204 No Content
```

#### 실패

| 상태 | 조건 |
|---|---|
| `401 Unauthorized` | 세션 인증 실패 |
