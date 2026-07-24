"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { Mascot } from "./Mascot";
import { WidgetGrid } from "./WidgetGrid";
import { ChatMessage } from "./ChatMessage";
import { TypingDots } from "./TypingDots";
import { ChatInput } from "./ChatInput";
import { useChat } from "@/features/chat/useChat";
import { useAuthStore } from "@/store/auth-store";
import { logout as logoutApi } from "@/lib/api/auth";

/**
 * 조립된 채팅 화면 (Handoff §3 반응형).
 * 모바일: 단일 컬럼(마스코트→위젯→챗→입력) / 데스크톱(md): 좌 사이드바(마스코트·위젯) + 우 챗 컬럼.
 * 공유 컴포넌트 재사용, 셸만 브레이크포인트로 분기.
 */
export function ChatScreen() {
  const { messages, pending, draft, setDraft, loading, send, pickWidget } =
    useChat();
  const scrollRef = useRef<HTMLDivElement>(null);
  const router = useRouter();
  const clearAuth = useAuthStore((s) => s.logout);

  // 새 메시지/타이핑 시 하단 고정
  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages, pending]);

  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      /* 멱등: 실패해도 로컬 정리 진행 */
    }
    clearAuth();
    router.replace("/login");
  };

  return (
    // 데스크톱: 캔버스 배경 위 중앙 정렬 폰 프레임 (Chat 시안). 모바일: 풀스크린.
    <div className="flex min-h-dvh flex-col bg-canvas md:items-center md:justify-center md:p-6">
      <div className="flex h-dvh w-full flex-col overflow-hidden bg-white md:h-[840px] md:max-h-[calc(100dvh-3rem)] md:w-[460px] md:rounded-[36px] md:border md:border-line md:shadow-[0_24px_48px_-16px_rgba(20,22,28,0.18)]">
        <header className="flex items-center justify-between border-b border-hairline px-6 py-3">
          <span className="font-mono text-[15px] tracking-wide text-muted">
            브라보
          </span>
          <button
            type="button"
            onClick={handleLogout}
            className="text-sm text-muted transition-colors hover:text-ink"
          >
            로그아웃
          </button>
        </header>

        {/* 마스코트 → 위젯 → 챗 → 입력 */}
        <div className="flex flex-col items-center gap-4 px-6 pt-4">
          <Mascot height={120} />
          <WidgetGrid onPick={pickWidget} />
        </div>

        <div
          ref={scrollRef}
          className="flex min-h-0 flex-1 flex-col gap-[13px] overflow-y-auto px-6 py-4"
        >
          {loading ? (
            <div className="m-auto text-sm text-faint">대화를 불러오는 중…</div>
          ) : (
            messages.map((m) => (
              <ChatMessage key={m.id} message={m} onSelectOption={send} />
            ))
          )}
          {pending && <TypingDots />}
        </div>

        <div className="border-t border-hairline bg-white px-6 py-4">
          <ChatInput
            value={draft}
            onChange={setDraft}
            onSend={() => send(draft)}
            disabled={pending}
          />
        </div>
      </div>
    </div>
  );
}
