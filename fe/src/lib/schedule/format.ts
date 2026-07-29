import type { Schedule } from "@/types/api/schedule";

/**
 * scheduledAt(UTC)의 KST 해석·표시는 전부 이 유틸을 통한다 (계약: KST 변환은 FE 책임).
 * dateKey 는 "YYYY-MM-DD" (KST 기준).
 */

const KST_DATE = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

const KST_TIME = new Intl.DateTimeFormat("en-GB", {
  timeZone: "Asia/Seoul",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
});

const DAY_KO = ["일", "월", "화", "수", "목", "금", "토"];

const pad = (n: number) => String(n).padStart(2, "0");

/** UTC instant → KST 파츠. */
export function toKstParts(iso: string): {
  dateKey: string; // "2026-07-29"
  time: string; // "15:00"
} {
  const d = new Date(iso);
  return { dateKey: KST_DATE.format(d), time: KST_TIME.format(d) };
}

/** 오늘(KST) dateKey. */
export function todayKst(): string {
  return KST_DATE.format(new Date());
}

/** dateKey + n일 (날짜 산술만 — 타임존 무관). */
export function addDays(dateKey: string, n: number): string {
  const [y, m, d] = dateKey.split("-").map(Number);
  const dt = new Date(Date.UTC(y, m - 1, d + n));
  return `${dt.getUTCFullYear()}-${pad(dt.getUTCMonth() + 1)}-${pad(dt.getUTCDate())}`;
}

/** dateKey 의 요일 (한글 1자). */
export function dayOfWeekKo(dateKey: string): string {
  const [y, m, d] = dateKey.split("-").map(Number);
  return DAY_KO[new Date(Date.UTC(y, m - 1, d)).getUTCDay()];
}

/** 기본 조회 범위 — 오늘(KST)부터 7일 (계약 §2-5 기본값과 동일). */
export function weekRange(): { from: string; to: string } {
  const from = todayKst();
  return { from, to: addDays(from, 6) };
}

/** "2026-07-29" → "7.29" */
export function shortDate(dateKey: string): string {
  const [, m, d] = dateKey.split("-");
  return `${Number(m)}.${d}`;
}

/** "7.29 – 8.04" */
export function rangeLabel(from: string, to: string): string {
  return `${shortDate(from)} – ${shortDate(to)}`;
}

/** 오늘/내일/M월 D일 상대 라벨. */
export function dateLabel(dateKey: string, today: string = todayKst()): string {
  if (dateKey === today) return "오늘";
  if (dateKey === addDays(today, 1)) return "내일";
  const [, m, d] = dateKey.split("-").map(Number);
  return `${m}월 ${d}일`;
}

export interface ScheduleGroup {
  dateKey: string;
  day: string;
  items: Schedule[];
}

/** KST 날짜 오름차순 그룹핑 (그룹 내 시간 오름차순). API 는 최신순이므로 재정렬. */
export function groupByKstDate(schedules: Schedule[]): ScheduleGroup[] {
  const sorted = [...schedules].sort((a, b) =>
    a.scheduledAt.localeCompare(b.scheduledAt),
  );
  const groups = new Map<string, Schedule[]>();
  for (const s of sorted) {
    const { dateKey } = toKstParts(s.scheduledAt);
    if (!groups.has(dateKey)) groups.set(dateKey, []);
    groups.get(dateKey)!.push(s);
  }
  return [...groups.entries()].map(([dateKey, items]) => ({
    dateKey,
    day: dayOfWeekKo(dateKey),
    items,
  }));
}

/** KST 날짜+시간 입력 → UTC ISO instant (등록/수정 폼 → BE 전송용). */
export function kstToUtcIso(dateKey: string, time: string): string {
  return new Date(`${dateKey}T${time}:00+09:00`).toISOString();
}
