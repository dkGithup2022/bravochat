"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import { useHydrated } from "./useHydrated";

/** 미인증이면 로그인 화면으로. 채팅 화면 등 보호 라우트에서 사용. */
export function useRequireAuth(opts?: { skip?: boolean }): {
  token: string | null;
  isReady: boolean;
} {
  const router = useRouter();
  const hydrated = useHydrated();
  const token = useAuthStore((s) => s.token);

  useEffect(() => {
    if (hydrated && !token && !opts?.skip) {
      router.replace("/login");
    }
  }, [hydrated, token, opts?.skip, router]);

  return { token, isReady: hydrated && !!token };
}
