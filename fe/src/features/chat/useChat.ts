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

// 렌더 키용 로컬 id. crypto.randomUUID 는 비보안 컨텍스트(LAN http)에 없어 사용 불가.
let localIdSeq = 0;
const newLocalId = () => `local-${++localIdSeq}`;

/**
 * 채팅 상태·플로우 (§S2 이력 복원, §S3 전송 루프).
 * - 마운트 시 최근 20턴 복원, 실패 시 loadError + reload 로 재시도
 * - 전송: 유저 버블 낙관적 렌더 → pending(타이핑) → 응답 append, 실패 시 버블 롤백(§S3-5·6)
 * - 401 은 client.ts 가 전역 이벤트로 처리(AuthProvider)
 */
export function useChat() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [pending, setPending] = useState(false);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let ignore = false; // 언마운트/재실행 후 늦게 도착한 응답이 메시지를 덮어쓰지 않게 가드
    (async () => {
      setLoading(true);
      setLoadError(false);
      try {
        const res = await getRecentTurns(20);
        if (!ignore) setMessages(turnsToMessages(res.turns));
      } catch {
        if (!ignore) setLoadError(true);
      } finally {
        if (!ignore) setLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  const send = useCallback(
    async (text: string, opts?: { restoreDraft?: boolean }) => {
      const trimmed = text.trim();
      if (!trimmed || pending) return;

      // 낙관적 유저 버블
      const localId = newLocalId();
      setMessages((m) => [...m, { id: localId, who: "user", text: trimmed }]);
      setPending(true);

      try {
        const res = await sendMessage(trimmed);
        setMessages((m) => [
          ...m,
          { id: `${res.turnId}-b`, who: "bot", text: res.message },
        ]);
      } catch (error) {
        // 롤백 + 입력 경로면 draft 복원으로 재시도 유도 (§S3-5·6)
        setMessages((m) => m.filter((msg) => msg.id !== localId));
        if (opts?.restoreDraft) setDraft((d) => (d.trim() ? d : trimmed));
        showErrorToast(error, "응답 생성에 실패했습니다");
      } finally {
        setPending(false);
      }
    },
    [pending],
  );

  /** 입력바 전송 — draft 를 비우되 실패 시 send 가 복원한다. */
  const sendDraft = useCallback(() => {
    const text = draft;
    setDraft("");
    void send(text, { restoreDraft: true });
  }, [draft, send]);

  /** 위젯 클릭 — 작성 중인 draft 는 건드리지 않는다. */
  const pickWidget = useCallback((prompt: string) => void send(prompt), [send]);

  return {
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
  };
}
