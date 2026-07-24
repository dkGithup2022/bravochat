"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { login as loginApi } from "@/lib/api/auth";
import { useAuthStore } from "@/store/auth-store";
import { useHydrated } from "@/hooks/useHydrated";
import { showErrorToast } from "@/lib/api/error-toast";
import { Mascot } from "@/components/chat/Mascot";

export default function LoginPage() {
  const router = useRouter();
  const hydrated = useHydrated();
  const token = useAuthStore((s) => s.token);
  const setAuth = useAuthStore((s) => s.login);

  const [username, setUsername] = useState("tester");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 이미 로그인된 상태면 홈으로
  useEffect(() => {
    if (hydrated && token) router.replace("/");
  }, [hydrated, token, router]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    try {
      const t = await loginApi(username, password);
      setAuth(t, username);
      router.replace("/");
    } catch (error) {
      showErrorToast(error, "로그인에 실패했습니다");
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
          onChange={(e) => setUsername(e.target.value)}
          placeholder="아이디"
          autoComplete="username"
          aria-label="아이디"
          className="rounded-field border border-line bg-surface px-4 py-3 text-base text-ink outline-none focus:border-ink"
        />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호"
          autoComplete="current-password"
          aria-label="비밀번호"
          className="rounded-field border border-line bg-surface px-4 py-3 text-base text-ink outline-none focus:border-ink"
        />
        <button
          type="submit"
          disabled={submitting}
          className="rounded-field bg-ink py-3 text-base font-bold text-white transition-opacity disabled:opacity-50"
        >
          {submitting ? "로그인 중…" : "로그인"}
        </button>
      </form>

      <p className="font-mono text-[13px] text-faint">tester / password1234</p>
    </main>
  );
}
