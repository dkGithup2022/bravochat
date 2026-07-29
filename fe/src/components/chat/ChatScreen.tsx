"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronDown, ChevronUp } from "lucide-react";
import { toast } from "sonner";
import { HeaderMenu } from "@/components/ui/HeaderMenu";
import { Mascot } from "./Mascot";
import { WidgetGrid } from "./WidgetGrid";
import { ChatMessage } from "./ChatMessage";
import { TypingDots } from "./TypingDots";
import { ChatInput } from "./ChatInput";
import { useChat } from "@/features/chat/useChat";
import { useAuthStore } from "@/store/auth-store";
import { logout as logoutApi } from "@/lib/api/auth";
import { getTranscript } from "@/lib/api/chat";
import { showErrorToast } from "@/lib/api/error-toast";

// 디버그 도구는 개발 모드에서만 노출 (BE 스펙: 제품 화면 비노출)
const IS_DEV = process.env.NODE_ENV === "development";

/**
 * 조립된 채팅 화면 (Handoff §3 반응형).
 * 모바일: 단일 컬럼(마스코트→위젯→챗→입력) / 데스크톱(md): 좌 사이드바(마스코트·위젯) + 우 챗 컬럼.
 * 공유 컴포넌트 재사용, 셸만 브레이크포인트로 분기.
 */
export function ChatScreen() {
  const {
    messages,
    pending,
    draft,
    setDraft,
    loading,
    loadError,
    reload,
    send,
    sendDraft,
    pickWidget,
  } = useChat();
  const scrollRef = useRef<HTMLDivElement>(null);
  const [panelOpen, setPanelOpen] = useState(true);
  const router = useRouter();
  const clearAuth = useAuthStore((s) => s.logout);
  const username = useAuthStore((s) => s.username);

  // 새 메시지/타이핑 시 하단 고정
  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages, pending]);

  const [copying, setCopying] = useState(false);

  /** [디버그] 전체 대화 전문을 받아 클립보드에 복사. */
  const handleDebugCopy = async () => {
    if (copying) return;
    setCopying(true);
    try {
      const transcript = await getTranscript();
      await navigator.clipboard.writeText(transcript);
      toast.success(`대화 전문 복사됨 (${transcript.length.toLocaleString()}자)`);
    } catch (error) {
      showErrorToast(error, "대화 전문을 복사하지 못했습니다");
    } finally {
      setCopying(false);
    }
  };

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
      <div className="flex h-dvh w-full flex-col overflow-hidden bg-white md:h-[calc(100dvh-3rem)] md:w-[520px] md:rounded-[36px] md:border md:border-line md:shadow-[0_24px_48px_-16px_rgba(20,22,28,0.18)]">
        <header className="flex items-center justify-between border-b border-hairline px-6 py-3">
          <span className="font-mono text-[15px] tracking-wide text-muted">
            브라보
          </span>
          <div className="flex items-center gap-3">
            {IS_DEV && (
              <button
                type="button"
                onClick={handleDebugCopy}
                disabled={copying}
                className="font-mono text-[12px] text-faint transition-colors hover:text-ink disabled:opacity-50"
              >
                {copying ? "복사 중…" : "[디버그-복사]"}
              </button>
            )}
            {username && (
              <span className="text-sm text-faint">{username} 님</span>
            )}
            <HeaderMenu onLogout={handleLogout} />
          </div>
        </header>

        {/* 마스코트 → 위젯 (접기 가능) → 챗 → 입력 */}
        {panelOpen && (
          <div className="flex flex-col items-center gap-3 px-6 pt-3">
            <Mascot height={96} />
            <WidgetGrid onPick={pickWidget} disabled={pending || loading} />
          </div>
        )}
        <button
          type="button"
          onClick={() => setPanelOpen((o) => !o)}
          aria-expanded={panelOpen}
          aria-label={panelOpen ? "위젯 접기" : "위젯 펼치기"}
          className="flex h-6 w-full flex-none items-center justify-center border-b border-hairline text-faint transition-colors hover:text-ink"
        >
          {panelOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </button>

        <div
          ref={scrollRef}
          className="flex min-h-0 flex-1 flex-col gap-[13px] overflow-y-auto px-6 py-4 md:gap-[10px] md:py-3"
        >
          {loading ? (
            <div className="m-auto text-sm text-faint">대화를 불러오는 중…</div>
          ) : loadError ? (
            <div className="m-auto flex flex-col items-center gap-3">
              <p className="text-sm text-faint">대화를 불러오지 못했습니다</p>
              <button
                type="button"
                onClick={reload}
                className="rounded-field border border-line px-4 py-2 text-sm font-bold text-ink transition-colors hover:border-ink"
              >
                다시 시도
              </button>
            </div>
          ) : messages.length === 0 ? (
            <div className="m-auto flex flex-col items-center gap-1 text-center">
              <p className="text-base font-bold text-ink">
                안녕하세요! 무엇이든 편하게 말씀해 주세요
              </p>
              <p className="text-sm text-faint">
                위의 위젯을 눌러 시작할 수도 있어요
              </p>
            </div>
          ) : (
            messages.map((m) => (
              <ChatMessage key={m.id} message={m} onSelectOption={send} />
            ))
          )}
          {pending && <TypingDots />}
        </div>

        <div className="border-t border-hairline bg-white px-6 py-4 md:py-3">
          <ChatInput
            value={draft}
            onChange={setDraft}
            onSend={sendDraft}
            onVoice={() => toast("음성 입력은 준비중입니다")}
            disabled={pending || loading}
          />
        </div>
      </div>
    </div>
  );
}
