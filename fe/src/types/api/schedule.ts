/** BE 계약 (specs/api-handoff.md §2-5~2-8, ScheduleApiController) 미러. */

export type ScheduleType = "HEALTH" | "PERSONAL" | "WORK" | "ETC";

/** GET/POST/PATCH /schedules 응답의 일정 객체 */
export interface Schedule {
  scheduleId: number;
  title: string;
  content: string | null;
  scheduleType: ScheduleType;
  scheduledAt: string; // ISO-8601 UTC — KST 표시는 FE 책임
  /**
   * 완료 여부 (BE doneAt != null 파생). 표시 전용 —
   * PATCH(UpdateScheduleRequest)에 done 필드가 없어 FE 에서 토글 불가.
   */
  done: boolean;
}

/** GET /schedules 응답 (최신순) */
export interface SchedulesResponse {
  schedules: Schedule[];
}

/** POST /schedules 요청 */
export interface CreateScheduleRequest {
  title: string; // 필수, ≤200자
  content?: string | null;
  scheduleType?: ScheduleType; // 생략/이외 값은 BE 가 ETC 로 흡수
  scheduledAt: string; // 필수, ISO-8601 UTC
}

/** PATCH /schedules/{id} 요청 — 보낸 필드만 변경 */
export interface UpdateScheduleRequest {
  title?: string;
  content?: string | null;
  scheduleType?: ScheduleType;
  scheduledAt?: string;
}
