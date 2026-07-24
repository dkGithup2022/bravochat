"use client";

import { useCallback, useEffect, useState } from "react";
import type { Message } from "@/types/data/chat";
import type { RecentTurn } from "@/types/api/chat";
import { getRecentTurns, sendMessage } from "@/lib/api/chat";
import { showErrorToast } from "@/lib/api/error-toast";

/** RecentTurn[] (오래된→최신) → 평면 Message[] (user 버블 + bot 버블). */
function turnsToMessages(turns: RecentTurn[]): Message[] {
  return turns.flatMap((t) => [
    { id: `${t.turnId}-u`, who: "user", text: t.userMessage } as Message,
    { id: `${t.turnId}-b`, who: "bot", text: t.assistantMessage } as Message,
  ]);
}

/**
 * 채팅 상태·플로우 (§S2 이력 복원, §S3 전송 루프).
 * - 마운트 시 최근 20턴 복원
 * - 전송: 유저 버블 낙관적 렌더 → pending(타이핑) → 응답 append
 * - 401 은 client.ts 가 전역 이벤트로 처리(AuthProvider)
 */
export function useChat() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [pending, setPending] = useState(false);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);

  const loadRecent = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getRecentTurns(20);
      setMessages(turnsToMessages(res.turns));
    } catch (error) {
      showErrorToast(error, "대화를 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRecent();
  }, [loadRecent]);

  const send = useCallback(
    async (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || pending) return;

      // 낙관적 유저 버블
      setMessages((m) => [
        ...m,
        { id: crypto.randomUUID(), who: "user", text: trimmed },
      ]);
      setDraft("");
      setPending(true);

      try {
        const res = await sendMessage(trimmed);
        setMessages((m) => [
          ...m,
          { id: `${res.turnId}-b`, who: "bot", text: res.message },
        ]);
      } catch (error) {
        showErrorToast(error, "응답 생성에 실패했습니다");
      } finally {
        setPending(false);
      }
    },
    [pending],
  );

  const pickWidget = useCallback((prompt: string) => send(prompt), [send]);

  return { messages, pending, draft, setDraft, loading, send, pickWidget };
}
