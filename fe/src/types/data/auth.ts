/** 로그인 세션 — 토큰은 클라이언트 보관(BE 핸드오프 §2-1). */
export interface Session {
  token: string;
  username: string;
}
