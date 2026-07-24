/** POST /auth/login 요청 바디 (BE 핸드오프 §2-1) */
export interface LoginRequest {
  username: string;
  password: string;
}
