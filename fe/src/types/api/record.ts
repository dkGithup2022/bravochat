/**
 * 기록(Record) — 과거 대화에서 자동 요약된 이력.
 * ⚠️ BE 미구현 — 아래는 가정 계약(DESIGN_PLAN.md). BE 확정 시 필드명 맞출 것.
 */

/** 타입 체계 미정 — 문자열로 받고 라벨 매핑만 교체 (분류A/B/C 는 자리표시자). */
export type RecordType = string;

export interface ChatRecord {
  recordId: number;
  date: string; // "YYYY-MM-DD" (일자, KST)
  summary: string; // 요약
  recordType: RecordType;
}

/** GET /records 응답 (최신순) — 가정 */
export interface RecordsResponse {
  records: ChatRecord[];
}
