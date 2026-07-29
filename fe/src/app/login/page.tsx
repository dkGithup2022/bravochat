"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { login as loginApi } from "@/lib/api/auth";
import { useAuthStore } from "@/store/auth-store";
import { useHydrated } from "@/hooks/useHydrated";
import { ApiError } from "@/lib/api/types";
import { Mascot } from "@/components/chat/Mascot";

// 시드 계정 힌트/프리필은 로컬 개발에서만 노출
const IS_DEV = process.env.NODE_ENV === "development";

export default function LoginPage() {
  const router = useRouter();
  const hydrated = useHydrated();
  const token = useAuthStore((s) => s.token);
  const setAuth = useAuthStore((s) => s.login);

  const [username, setUsername] = useState(IS_DEV ? "tester" : "");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // 이미 로그인된 상태면 홈으로
  useEffect(() => {
    if (hydrated && token) router.replace("/");
  }, [hydrated, token, router]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setErrorMsg(null);
    try {
      const t = await loginApi(username, password);
      setAuth(t, username);
      router.replace("/");
    } catch (error) {
      // 로그인 401 은 세션만료가 아닌 인증 실패 — 전역 토스트 규칙(401 무시) 대신 인라인 표시 (§S1-4)
      if (error instanceof ApiError && error.status === 401) {
        setErrorMsg("아이디 또는 비밀번호가 올바르지 않습니다");
      } else if (error instanceof ApiError && error.message) {
        setErrorMsg(error.message);
      } else {
        setErrorMsg("로그인에 실패했습니다");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-6 px-6">
      <Mascot />
      <h1 className="text-2xl font-black text-ink">브라보 챗</h1>

      <form
        onSubmit={handleSubmit}
        className="flex w-full max-w-xs flex-col gap-3"
      >
        <input
          value={username}
          onChange={(e) => {
            setUsername(e.target.value);
            setErrorMsg(null);
          }}
          placeholder="아이디"
          autoComplete="username"
          autoFocus
          aria-label="아이디"
          className="rounded-field border border-line bg-surface px-4 py-3 text-base text-ink outline-none focus:border-ink"
        />
        <input
          type="password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value);
            setErrorMsg(null);
          }}
          placeholder="비밀번호"
          autoComplete="current-password"
          aria-label="비밀번호"
          className="rounded-field border border-line bg-surface px-4 py-3 text-base text-ink outline-none focus:border-ink"
        />
        {errorMsg && (
          <p role="alert" className="text-sm text-red-600">
            {errorMsg}
          </p>
        )}
        <button
          type="submit"
          disabled={submitting || !username.trim() || !password.trim()}
          className="rounded-field bg-ink py-3 text-base font-bold text-white transition-opacity disabled:opacity-50"
        >
          {submitting ? "로그인 중…" : "로그인"}
        </button>
      </form>

      {IS_DEV && (
        <p className="font-mono text-[13px] text-faint">tester / password1234</p>
      )}
    </main>
  );
}
