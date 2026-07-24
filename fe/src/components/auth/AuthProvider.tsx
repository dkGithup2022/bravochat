"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";

/**
 * 세션 만료 횡단 처리 (§S4).
 * 모든 인증 호출에서 401/403 → client.ts 가 발화한 auth:unauthorized 이벤트를 듣고
 * 토큰 폐기 + 로그인 화면 리다이렉트.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const logout = useAuthStore((s) => s.logout);
  const router = useRouter();

  useEffect(() => {
    const handler = () => {
      logout();
      router.replace("/login");
    };
    window.addEventListener("auth:unauthorized", handler);
    return () => window.removeEventListener("auth:unauthorized", handler);
  }, [logout, router]);

  return <>{children}</>;
}
