"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import { HeaderMenu } from "@/components/ui/HeaderMenu";
import { useRecords } from "@/features/record/useRecords";
import { useAuthStore } from "@/store/auth-store";
import { logout as logoutApi } from "@/lib/api/auth";
import { shortDate } from "@/lib/schedule/format";
import {
  groupByDateDesc,
  pastDateLabel,
  recordTypeLabel,
} from "@/lib/record/format";
import type { ChatRecord } from "@/types/api/record";

/**
 * 기록 화면 (타임라인) — 과거 대화에서 자동 요약된 이력, 최신순.
 * 조작은 삭제만. ⚠️ BE 미구현 — 항상 목 데이터 (상단 안내 1줄 노출).
 */
export function RecordScreen() {
  const { records, loading, loadError, mutating, reload, remove } = useRecords();
  const [openId, setOpenId] = useState<number | null>(null);

  const router = useRouter();
  const clearAuth = useAuthStore((s) => s.logout);
  const username = useAuthStore((s) => s.username);

  const groups = useMemo(() => groupByDateDesc(records), [records]);

  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      /* 멱등: 실패해도 로컬 정리 진행 */
    }
    clearAuth();
    router.replace("/login");
  };

  const handleDelete = async (record: ChatRecord) => {
    if (
      !window.confirm(`이 기록을 삭제할까요?\n"${record.summary.slice(0, 40)}…"`)
    )
      return;
    const okDone = await remove(record.recordId);
    if (okDone && openId === record.recordId) setOpenId(null);
  };

  return (
    <div className="flex min-h-dvh flex-col bg-canvas md:items-center md:justify-center md:p-6">
      <div className="flex h-dvh w-full flex-col overflow-hidden bg-white md:h-[calc(100dvh-3rem)] md:w-[520px] md:rounded-[36px] md:border md:border-line md:shadow-[0_24px_48px_-16px_rgba(20,22,28,0.18)]">
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

        {/* BE 미구현 안내 — 실제 환경에서도 목 데이터만 표시 중 */}
        <p className="flex-none border-b border-hairline bg-surface py-[5px] text-center font-mono text-[11px] text-muted">
          ⚠︎ 현재 mock 데이터 표시 중 — BE 기록 API 준비 전
        </p>

        {/* 요약 바 — 일정 화면과 같은 자리 문법 */}
        <div className="flex flex-none items-center justify-between border-b border-hairline px-6 py-[10px]">
          <span className="font-mono text-[13px] text-muted">
            기록{!loading && !loadError && ` · ${records.length}건`}
          </span>
          <span className="font-mono text-[11px] text-faint">최신순</span>
        </div>

        {/* 타임라인 */}
        <div className="min-h-0 flex-1 overflow-y-auto px-6 pb-6 pt-4">
          {loading ? (
            <div className="flex h-full items-center justify-center text-sm text-faint">
              기록을 불러오는 중…
            </div>
          ) : loadError ? (
            <div className="flex h-full flex-col items-center justify-center gap-3">
              <p className="text-sm text-faint">기록을 불러오지 못했습니다</p>
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
              <p className="text-base font-bold text-ink">아직 기록이 없어요</p>
              <p className="text-sm text-faint">
                대화하다 보면 의미 있는 이력이 자동으로 쌓여요
              </p>
            </div>
          ) : (
            <div className="relative border-l-[1.5px] border-line pl-5">
              {groups.map((g) => (
                <section key={g.dateKey} className="pb-5">
                  <h3 className="relative flex items-baseline gap-2 pb-2">
                    <span className="absolute -left-[26.5px] top-[5px] h-[11px] w-[11px] rounded-full border-[2.5px] border-ink bg-white" />
                    <span className="text-[14px] font-bold text-ink">
                      {pastDateLabel(g.dateKey)}
                    </span>
                    <span className="font-mono text-[11px] text-faint">
                      {shortDate(g.dateKey)} ({g.day})
                    </span>
                  </h3>
                  <div className="flex flex-col gap-[12px]">
                    {g.items.map((r) => {
                      const open = openId === r.recordId;
                      return (
                        <div
                          key={r.recordId}
                          role="button"
                          tabIndex={0}
                          onClick={() => setOpenId(open ? null : r.recordId)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter" || e.key === " ") {
                              e.preventDefault();
                              setOpenId(open ? null : r.recordId);
                            }
                          }}
                          aria-expanded={open}
                          className="group cursor-pointer outline-none"
                        >
                          <p
                            className={`text-[14px] leading-relaxed text-ink ${
                              open ? "" : "line-clamp-2"
                            }`}
                          >
                            {r.summary}
                          </p>
                          <div className="flex items-center gap-[10px] pt-[3px]">
                            <span className="font-mono text-[10px] text-faint">
                              {recordTypeLabel(r.recordType)}
                            </span>
                            <button
                              type="button"
                              aria-label="기록 삭제"
                              disabled={mutating}
                              onClick={(e) => {
                                e.stopPropagation();
                                void handleDelete(r);
                              }}
                              className={`text-faint transition-colors hover:text-ink disabled:opacity-40 ${
                                open ? "" : "hidden group-hover:block"
                              }`}
                            >
                              <Trash2 size={13} />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </section>
              ))}
              <p className="pt-1 text-center font-mono text-[11px] text-faint">
                대화에서 의미 있는 이력이 자동으로 기록됩니다
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
