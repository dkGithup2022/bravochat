# Data Models

| 모델 | 위치 | 설명 |
|------|------|------|
| `Message` (union) | `src/types/data/chat.ts` | user·bot·schedule·options. 현재 be→user/bot만, schedule/options는 위젯 목/향후 대비 |
| `Widget` | `src/types/data/chat.ts` | 위젯 2×2 (icon,label,meta,prompt,disabled) |
| `Session` | `src/types/data/auth.ts` | token, username |
| `SendMessageResponse` | `src/types/api/chat.ts` | POST /chat/turns 응답 |
| `RecentTurn` / `RecentTurnsResponse` | `src/types/api/chat.ts` | GET /chat/turns/recent |
| `LoginRequest` | `src/types/api/auth.ts` | POST /auth/login 바디 |
| `ProxyRequest` / `ProxyResponse` / `ApiError` | `src/lib/api/types.ts` | 프록시 envelope + 정규화 에러 |
