import { apiDelete, apiGet, apiPatch, apiPost } from "./client";
import type {
  CreateScheduleRequest,
  Schedule,
  SchedulesResponse,
  UpdateScheduleRequest,
} from "@/types/api/schedule";

/** GET /schedules?from&to&size → 기간 내 일정 최신순 (§2-5). size 는 서버가 1~100 으로 보정. */
export function getSchedules(params?: {
  from?: string; // YYYY-MM-DD (KST), 생략 시 오늘
  to?: string; // 생략 시 from+6일
  size?: number;
}): Promise<SchedulesResponse> {
  const query: Record<string, string | number> = {};
  if (params?.from) query.from = params.from;
  if (params?.to) query.to = params.to;
  if (params?.size !== undefined) query.size = Math.trunc(params.size);
  return apiGet<SchedulesResponse>("/schedules", query);
}

/** POST /schedules → 201 생성된 일정 (§2-6). */
export function createSchedule(input: CreateScheduleRequest): Promise<Schedule> {
  return apiPost<Schedule>("/schedules", input);
}

/**
 * PATCH /schedules/{id} → 변경 후 일정 (§2-7).
 * ⚠️ 교체 방식 — 응답의 scheduleId 가 바뀐다. 호출측은 반드시 새 객체로 로컬 상태 교체.
 */
export function patchSchedule(
  scheduleId: number,
  input: UpdateScheduleRequest,
): Promise<Schedule> {
  return apiPatch<Schedule>(`/schedules/${scheduleId}`, input);
}

/** DELETE /schedules/{id} → 204 (§2-8). */
export function deleteSchedule(scheduleId: number): Promise<null> {
  return apiDelete<null>(`/schedules/${scheduleId}`);
}
