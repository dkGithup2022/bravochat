import type { ChatRecord } from "@/types/api/record";
import { addDays, dayOfWeekKo, todayKst } from "@/lib/schedule/format";

/** 기록은 과거 이력 — 최신 날짜가 위로 (일정과 반대). 날짜 유틸은 schedule/format 공용. */

/** 오늘/어제/M월 D일 상대 라벨. */
export function pastDateLabel(dateKey: string, today: string = todayKst()): string {
  if (dateKey === today) return "오늘";
  if (dateKey === addDays(today, -1)) return "어제";
  const [, m, d] = dateKey.split("-").map(Number);
  return `${m}월 ${d}일`;
}

/** 타입 라벨 — 체계 미정, 자리표시자 매핑. BE 확정 시 여기만 교체. */
export function recordTypeLabel(type: string): string {
  const known: Record<string, string> = { A: "분류A", B: "분류B", C: "분류C" };
  return known[type] ?? type;
}

export interface RecordGroup {
  dateKey: string;
  day: string;
  items: ChatRecord[];
}

/** 최신 날짜가 위로 오는 그룹핑 (동일 일자는 id 내림차순). */
export function groupByDateDesc(records: ChatRecord[]): RecordGroup[] {
  const sorted = [...records].sort(
    (a, b) => b.date.localeCompare(a.date) || b.recordId - a.recordId,
  );
  const groups = new Map<string, ChatRecord[]>();
  for (const r of sorted) {
    if (!groups.has(r.date)) groups.set(r.date, []);
    groups.get(r.date)!.push(r);
  }
  return [...groups.entries()].map(([dateKey, items]) => ({
    dateKey,
    day: dayOfWeekKo(dateKey),
    items,
  }));
}
