import { apiLogin, apiDelete } from "./client";

/** POST /auth/login → 세션 토큰 반환 (§2-1). */
export function login(username: string, password: string): Promise<string> {
  return apiLogin({ username, password });
}

/** DELETE /auth/session → 로그아웃 (멱등, §2-2). */
export async function logout(): Promise<void> {
  await apiDelete<null>("/auth/session");
}
