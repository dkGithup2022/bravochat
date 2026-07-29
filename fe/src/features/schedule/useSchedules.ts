"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import {
  createSchedule,
  deleteSchedule,
  getSchedules,
  patchSchedule,
} from "@/lib/api/schedule";
import { ApiError } from "@/lib/api/types";
import { showErrorToast } from "@/lib/api/error-toast";
import { weekRange } from "@/lib/schedule/format";
import type {
  CreateScheduleRequest,
  Schedule,
  UpdateScheduleRequest,
} from "@/types/api/schedule";

/**
 * 일정 화면 상태 — 오늘(KST)부터 7일 조회 + 등록/수정/삭제.
 * PATCH 는 교체 방식이라 응답의 새 scheduleId 로 로컬 상태를 통째로 교체한다 (§2-7).
 * 404(다른 기기/챗에서 변경된 경우) → 목록 재조회로 동기화 (S6-5).
 */
export function useSchedules() {
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  const range = useMemo(() => weekRange(), []);

  useEffect(() => {
    let ignore = false; // 언마운트/재실행 후 늦게 도착한 응답이 목록을 덮어쓰지 않게 가드
    (async () => {
      setLoading(true);
      setLoadError(false);
      try {
        const res = await getSchedules({
          from: range.from,
          to: range.to,
          size: 100,
        });
        if (!ignore) setSchedules(res.schedules);
      } catch (error) {
        if (!ignore) {
          setLoadError(true);
          showErrorToast(error, "일정을 불러오지 못했습니다");
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, [range, reloadKey]);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  /** 404 → 죽은 참조: 안내 후 재조회. 그 외는 일반 에러 토스트. */
  const handleMutationError = useCallback(
    (error: unknown, fallback: string) => {
      if (error instanceof ApiError && error.status === 404) {
        toast.error("일정을 찾을 수 없어 목록을 새로고침했습니다");
        reload();
        return;
      }
      showErrorToast(error, fallback);
    },
    [reload],
  );

  const create = useCallback(
    async (input: CreateScheduleRequest): Promise<boolean> => {
      setMutating(true);
      try {
        const created = await createSchedule(input);
        setSchedules((prev) => [created, ...prev]);
        return true;
      } catch (error) {
        handleMutationError(error, "일정을 등록하지 못했습니다");
        return false;
      } finally {
        setMutating(false);
      }
    },
    [handleMutationError],
  );

  const update = useCallback(
    async (scheduleId: number, input: UpdateScheduleRequest): Promise<boolean> => {
      setMutating(true);
      try {
        const replaced = await patchSchedule(scheduleId, input);
        // 기존 ID 는 죽은 참조 — 응답 객체(새 ID)로 교체
        setSchedules((prev) =>
          prev.map((s) => (s.scheduleId === scheduleId ? replaced : s)),
        );
        return true;
      } catch (error) {
        handleMutationError(error, "일정을 수정하지 못했습니다");
        return false;
      } finally {
        setMutating(false);
      }
    },
    [handleMutationError],
  );

  const remove = useCallback(
    async (scheduleId: number): Promise<boolean> => {
      setMutating(true);
      try {
        await deleteSchedule(scheduleId);
        setSchedules((prev) => prev.filter((s) => s.scheduleId !== scheduleId));
        return true;
      } catch (error) {
        handleMutationError(error, "일정을 삭제하지 못했습니다");
        return false;
      } finally {
        setMutating(false);
      }
    },
    [handleMutationError],
  );

  return {
    schedules,
    range,
    loading,
    loadError,
    mutating,
    reload,
    create,
    update,
    remove,
  };
}
