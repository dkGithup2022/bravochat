# API Routes (Next route handlers)

| 경로 | 파일 | 메서드 | 설명 |
|------|------|--------|------|
| `/api/proxy` | `src/app/api/proxy/route.ts` | POST | BFF 프록시. `{method,path,body,params}` → `BACKEND_API_URL`(:8080). Authorization 요청/응답 헤더 릴레이(Bearer). same-origin → CORS 회피 |

## 목/실 전환
- `NEXT_PUBLIC_USE_MOCK=true` → `lib/api/mock/index.ts` 인프로세스 목 (프록시 안 탐)
- `NEXT_PUBLIC_USE_MOCK=false` → 위 `/api/proxy` 통해 실제 BE 호출
- 레퍼런스: `play_claude_code/ui_gen/share-ai-setup.dev` (쿠키 세션 → Bravo Bearer 로 적응)

## API 접근 계층
- `lib/api/client.ts` — `apiGet/apiPost/apiDelete/apiLogin`, mock|proxy 분기, Bearer 주입, 401→`auth:unauthorized` 이벤트
- `lib/api/auth.ts` — `login(username,password)`, `logout()`
- `lib/api/chat.ts` — `sendMessage(message)`, `getRecentTurns(size)`
- `lib/api/error-toast.ts` — `showErrorToast` (401/403 무시, sonner)
