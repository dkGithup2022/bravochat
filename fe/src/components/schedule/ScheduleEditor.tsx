"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";
import { kstToUtcIso, toKstParts, todayKst } from "@/lib/schedule/format";
import type { Schedule, ScheduleType } from "@/types/api/schedule";

const TYPES: Array<{ value: ScheduleType; label: string }> = [
  { value: "HEALTH", label: "건강" },
  { value: "PERSONAL", label: "개인" },
  { value: "WORK", label: "업무" },
  { value: "ETC", label: "기타" },
];

export interface ScheduleEditorValue {
  title: string;
  content: string | null;
  scheduleType: ScheduleType;
  scheduledAt: string; // UTC ISO
}

/**
 * 등록/수정 공용 바텀 시트 폼 — 폰 프레임 안 오버레이.
 * 날짜/시간은 KST 로 입력받아 전송 시 UTC 로 변환한다.
 */
export function ScheduleEditor({
  initial,
  busy,
  onSubmit,
  onClose,
}: {
  initial?: Schedule;
  busy: boolean;
  onSubmit: (value: ScheduleEditorValue) => void;
  onClose: () => void;
}) {
  const initialParts = initial ? toKstParts(initial.scheduledAt) : null;
  const [title, setTitle] = useState(initial?.title ?? "");
  const [content, setContent] = useState(initial?.content ?? "");
  const [scheduleType, setScheduleType] = useState<ScheduleType>(
    initial?.scheduleType ?? "ETC",
  );
  const [date, setDate] = useState(initialParts?.dateKey ?? todayKst());
  const [time, setTime] = useState(initialParts?.time ?? "09:00");
  const [titleError, setTitleError] = useState<string | null>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  const submit = () => {
    const trimmed = title.trim();
    if (!trimmed) {
      setTitleError("제목을 입력해 주세요");
      return;
    }
    if (trimmed.length > 200) {
      setTitleError("제목은 200자 이내여야 합니다");
      return;
    }
    if (!date || !time) return;
    onSubmit({
      title: trimmed,
      content: content.trim() || null,
      scheduleType,
      scheduledAt: kstToUtcIso(date, time),
    });
  };

  return (
    <div
      className="absolute inset-0 z-20 flex flex-col justify-end bg-ink/20"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={initial ? "일정 수정" : "새 일정 등록"}
        onClick={(e) => e.stopPropagation()}
        className="flex flex-col gap-3 rounded-t-[20px] border-t border-line bg-white px-6 pb-6 pt-5"
      >
        <h2 className="text-[15px] font-bold text-ink">
          {initial ? "일정 수정" : "새 일정"}
        </h2>

        <label className="flex flex-col gap-1">
          <span className="text-[12px] font-bold text-muted">제목</span>
          <input
            autoFocus
            value={title}
            maxLength={200}
            onChange={(e) => {
              setTitle(e.target.value);
              setTitleError(null);
            }}
            placeholder="무슨 일정인가요?"
            className={cn(
              "rounded-field border bg-surface px-3 py-2 text-[14px] text-ink outline-none placeholder:text-faint focus:border-ink",
              titleError ? "border-red-400" : "border-line",
            )}
          />
          {titleError && (
            <span className="text-[12px] text-red-500">{titleError}</span>
          )}
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-[12px] font-bold text-muted">
            상세 <span className="font-normal text-faint">(선택)</span>
          </span>
          <input
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="장소, 메모 등"
            className="rounded-field border border-line bg-surface px-3 py-2 text-[14px] text-ink outline-none placeholder:text-faint focus:border-ink"
          />
        </label>

        <div className="flex gap-2">
          <label className="flex flex-1 flex-col gap-1">
            <span className="text-[12px] font-bold text-muted">날짜</span>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="rounded-field border border-line bg-surface px-3 py-2 font-mono text-[13px] text-ink outline-none focus:border-ink"
            />
          </label>
          <label className="flex w-[120px] flex-col gap-1">
            <span className="text-[12px] font-bold text-muted">시간</span>
            <input
              type="time"
              value={time}
              onChange={(e) => setTime(e.target.value)}
              className="rounded-field border border-line bg-surface px-3 py-2 font-mono text-[13px] text-ink outline-none focus:border-ink"
            />
          </label>
        </div>

        <div className="flex flex-col gap-1">
          <span className="text-[12px] font-bold text-muted">분류</span>
          <div className="flex gap-[6px]">
            {TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                onClick={() => setScheduleType(t.value)}
                className={cn(
                  "flex-1 rounded-full py-[7px] font-mono text-[12px] transition-colors",
                  scheduleType === t.value
                    ? "bg-ink font-bold text-white"
                    : "border border-line text-muted hover:border-ink hover:text-ink",
                )}
              >
                {t.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex gap-2 pt-1">
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="flex-1 rounded-field border border-line py-[10px] text-[14px] text-muted transition-colors hover:border-ink hover:text-ink disabled:opacity-40"
          >
            취소
          </button>
          <button
            type="button"
            onClick={submit}
            disabled={busy}
            className="flex-[2] rounded-field bg-ink py-[10px] text-[14px] font-bold text-white transition-opacity hover:opacity-85 disabled:opacity-40"
          >
            {busy ? "저장 중…" : initial ? "저장" : "등록"}
          </button>
        </div>
      </div>
    </div>
  );
}
