import { apiGet, apiPost } from "./client";
import type { SendMessageResponse, RecentTurnsResponse } from "@/types/api/chat";

/** POST /chat/turns → 어시스턴트 최종 응답 (동기, §2-3). */
export function sendMessage(message: string): Promise<SendMessageResponse> {
  return apiPost<SendMessageResponse>("/chat/turns", { message });
}

/** GET /chat/turns/recent?size=20 → 최근 턴 (오래된→최신, §2-4). */
export function getRecentTurns(size = 20): Promise<RecentTurnsResponse> {
  return apiGet<RecentTurnsResponse>("/chat/turns/recent", { size });
}
