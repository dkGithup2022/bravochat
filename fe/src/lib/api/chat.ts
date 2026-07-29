import { apiGet, apiPost } from "./client";
import type { SendMessageResponse, RecentTurnsResponse } from "@/types/api/chat";

/** POST /chat/turns → 어시스턴트 최종 응답 (동기, §2-3). */
export function sendMessage(message: string): Promise<SendMessageResponse> {
  return apiPost<SendMessageResponse>("/chat/turns", { message });
}

/** GET /chat/turns/transcript → [디버그] 전체 대화 전문 (text/plain 문자열 통짜). */
export function getTranscript(): Promise<string> {
  return apiGet<string>("/chat/turns/transcript");
}

/** GET /chat/turns/recent?size=20 → 최근 턴 (오래된→최신, §2-4). size 허용 범위 1~20 으로 보정. */
export function getRecentTurns(size = 20): Promise<RecentTurnsResponse> {
  const clamped = Math.min(20, Math.max(1, Math.trunc(size)));
  return apiGet<RecentTurnsResponse>("/chat/turns/recent", { size: clamped });
}
