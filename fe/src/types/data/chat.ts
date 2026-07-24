/**
 * 채팅 도메인 모델 — Handoff §5 데이터 모델 기준.
 *
 * ⚠️ 현재 BE는 응답으로 plain text(`bot`)만 반환한다.
 *    schedule/options 는 향후 BE 툴/구조화 응답 대비 + FE 위젯 목 데모용.
 *    be→Message 매핑은 지금은 user/bot 두 타입만 사용.
 */
export type Message =
  | { id: string; who: "user"; text: string }
  | { id: string; who: "bot"; text: string }
  | { id: string; who: "schedule"; items: string[] }
  | { id: string; who: "options"; options: string[] };

export type MessageWho = Message["who"];

/** 위젯 2×2 (마스코트 하단). 클릭 시 prompt 를 유저 메시지로 주입. */
export interface Widget {
  id: string;
  icon: string;
  label: string;
  meta?: string;
  prompt: string;
  /** 미정(점선/회색) 자리표시자 여부 */
  disabled?: boolean;
}
