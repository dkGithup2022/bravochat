"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { HeaderMenu } from "@/components/ui/HeaderMenu";
import { ScheduleRow } from "./ScheduleRow";
import { ScheduleEditor } from "./ScheduleEditor";
import { useSchedules } from "@/features/schedule/useSchedules";
import { useAuthStore } from "@/store/auth-store";
import { logout as logoutApi } from "@/lib/api/auth";
import {
  dateLabel,
  groupByKstDate,
  rangeLabel,
  shortDate,
} from "@/lib/schedule/format";
import type { Schedule } from "@/types/api/schedule";

/**
 * 일정 화면 (컴팩트 아젠다) — 오늘(KST)부터 7일.
 * ChatScreen 과 동일한 폰 프레임 셸 + 헤더 챗/일정 탭.
 */
export function ScheduleScreen() {
  const {
    schedules,
    range,
    loading,
    loadError,
    mutating,
    reload,
    create,
    update,
    remove,
  } = useSchedules();

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<Schedule | null>(null);

  const router = useRouter();
  const clearAuth = useAuthStore((s) => s.logout);
  const username = useAuthStore((s) => s.username);

  const groups = useMemo(() => groupByKstDate(schedules), [schedules]);

  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      /* 멱등: 실패해도 로컬 정리 진행 */
    }
    clearAuth();
    router.replace("/login");
  };

  const openCreate = () => {
    setEditing(null);
    setEditorOpen(true);
  };

  const openEdit = (schedule: Schedule) => {
    setEditing(schedule);
    setEditorOpen(true);
  };

  const handleDelete = async (schedule: Schedule) => {
    if (!window.confirm(`"${schedule.title}" 일정을 삭제할까요?`)) return;
    const okDone = await remove(schedule.scheduleId);
    if (okDone) setSelectedId(null);
  };

  return (
    <div className="flex min-h-dvh flex-col bg-canvas md:items-center md:justify-center md:p-6">
      <div className="relative flex h-dvh w-full flex-col overflow-hidden bg-white md:h-[calc(100dvh-3rem)] md:w-[520px] md:rounded-[36px] md:border md:border-line md:shadow-[0_24px_48px_-16px_rgba(20,22,28,0.18)]">
        <header className="flex flex-none items-center justify-between border-b border-hairline px-6 py-3">
          <span className="font-mono text-[15px] tracking-wide text-muted">
            브라보
          </span>
          <div className="flex items-center gap-3">
            {username && (
              <span className="text-sm text-faint">{username} 님</span>
            )}
            <HeaderMenu onLogout={handleLogout} />
          </div>
        </header>

        {/* 기간 요약 + 등록 */}
        <div className="flex flex-none items-center justify-between border-b border-hairline px-6 py-[10px]">
          <span className="font-mono text-[13px] text-muted">
            {rangeLabel(range.from, range.to)}
            {!loading && !loadError && ` · ${schedules.length}건`}
          </span>
          <button
            type="button"
            onClick={openCreate}
            disabled={loading || loadError}
            className="flex items-center gap-1 rounded-field border border-line px-3 py-[6px] text-[13px] font-bold text-ink transition-colors hover:border-ink disabled:opacity-40"
          >
            <Plus size={14} strokeWidth={2.5} />새 일정
          </button>
        </div>

        {/* 아젠다 */}
        <div className="min-h-0 flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex h-full items-center justify-center text-sm text-faint">
              일정을 불러오는 중…
            </div>
          ) : loadError ? (
            <div className="flex h-full flex-col items-center justify-center gap-3">
              <p className="text-sm text-faint">일정을 불러오지 못했습니다</p>
              <button
                type="button"
                onClick={reload}
                className="rounded-field border border-line px-4 py-2 text-sm font-bold text-ink transition-colors hover:border-ink"
              >
                다시 시도
              </button>
            </div>
          ) : groups.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center gap-1 text-center">
              <p className="text-base font-bold text-ink">이번주 일정이 없어요</p>
              <p className="text-sm text-faint">
                새 일정 버튼이나 챗으로 등록해 보세요
              </p>
            </div>
          ) : (
            <>
              {groups.map((g) => (
                <section key={g.dateKey}>
                  <h3 className="sticky top-0 z-10 border-b border-hairline bg-surface px-6 py-[6px] font-mono text-[12px] text-muted">
                    {dateLabel(g.dateKey, range.from)} · {shortDate(g.dateKey)} (
                    {g.day})
                  </h3>
                  {g.items.map((s) => (
                    <ScheduleRow
                      key={s.scheduleId}
                      schedule={s}
                      selected={selectedId === s.scheduleId}
                      disabled={mutating}
                      onSelect={() =>
                        setSelectedId((prev) =>
                          prev === s.scheduleId ? null : s.scheduleId,
                        )
                      }
                      onEdit={() => openEdit(s)}
                      onDelete={() => void handleDelete(s)}
                    />
                  ))}
                </section>
              ))}
              <p className="px-6 py-5 text-center font-mono text-[11px] text-faint">
                오늘부터 7일 범위를 표시합니다
              </p>
            </>
          )}
        </div>

        {editorOpen && (
          <ScheduleEditor
            initial={editing ?? undefined}
            busy={mutating}
            onClose={() => setEditorOpen(false)}
            onSubmit={async (value) => {
              const okDone = editing
                ? await update(editing.scheduleId, value)
                : await create(value);
              if (okDone) {
                setEditorOpen(false);
                setSelectedId(null);
              }
            }}
          />
        )}
      </div>
    </div>
  );
}
