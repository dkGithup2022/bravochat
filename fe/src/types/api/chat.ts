/** BE 계약 (specs/api-handoff.md §2-3, §2-4) 미러. */

/** POST /chat/turns 응답 */
export interface SendMessageResponse {
  turnId: number;
  message: string;
  createdAt: string; // ISO-8601 UTC
}

/** GET /chat/turns/recent 의 개별 턴 */
export interface RecentTurn {
  turnId: number;
  userMessage: string;
  assistantMessage: string;
  createdAt: string;
}

/** GET /chat/turns/recent 응답 (오래된→최신) */
export interface RecentTurnsResponse {
  turns: RecentTurn[];
}
