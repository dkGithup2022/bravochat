import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * 세션 토큰 보관 — BE 핸드오프 §2-1 ("FE가 저장하고 이후 요청에 붙임").
 * persist(localStorage) 로 새로고침에도 유지. client.ts 가 getState().token 으로 읽어 Bearer 부착.
 */
interface AuthStore {
  token: string | null;
  username: string | null;
  login: (token: string, username: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      token: null,
      username: null,
      login: (token, username) => set({ token, username }),
      logout: () => set({ token: null, username: null }),
    }),
    { name: "bravo-auth" },
  ),
);
