"use client";

import { useCallback, useEffect, useState } from "react";
import { getRecords, deleteRecord } from "@/lib/api/record";
import { showErrorToast } from "@/lib/api/error-toast";
import type { ChatRecord } from "@/types/api/record";

/**
 * 기록 화면 상태 — 최신순 조회 + 삭제 (조작은 삭제만, 자동 생성 데이터).
 * ⚠️ 데이터는 BE 미구현으로 항상 목 (lib/api/record.ts).
 */
export function useRecords() {
  const [records, setRecords] = useState<ChatRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [mutating, setMutating] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let ignore = false;
    (async () => {
      setLoading(true);
      setLoadError(false);
      try {
        const res = await getRecords();
        if (!ignore) setRecords(res.records);
      } catch (error) {
        if (!ignore) {
          setLoadError(true);
          showErrorToast(error, "기록을 불러오지 못했습니다");
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    })();
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  const remove = useCallback(async (recordId: number): Promise<boolean> => {
    setMutating(true);
    try {
      await deleteRecord(recordId);
      setRecords((prev) => prev.filter((r) => r.recordId !== recordId));
      return true;
    } catch (error) {
      showErrorToast(error, "기록을 삭제하지 못했습니다");
      return false;
    } finally {
      setMutating(false);
    }
  }, []);

  return { records, loading, loadError, mutating, reload, remove };
}
