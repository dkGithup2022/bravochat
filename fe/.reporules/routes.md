# Page Routes (App Router)

| 경로 | 파일 | 설명 |
|------|------|------|
| `/` | `src/app/page.tsx` | 채팅 홈 (보호 라우트, useRequireAuth). ChatScreen 렌더 |
| `/login` | `src/app/login/page.tsx` | 로그인 (마스코트 + 폼). 성공 시 토큰 저장 → `/` |

## 화면 구성
- `ChatScreen`(`components/chat/`) — 중앙 정렬 단일 컬럼(Chat 시안): 헤더(브라보/로그아웃) → 마스코트 → 위젯 2×2 → 챗 스크롤 → 입력바.
- 상태·플로우: `features/chat/useChat.ts` (recent 복원, 낙관적 전송→pending→append, 위젯 prompt 주입).
- 미구현/후속: Handoff §3 데스크톱 사이드바 분할(현재 단일 컬럼), 음성 입력 동작, schedule/options 실 응답(BE 툴 대기).
