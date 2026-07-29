"use client";

import { Check, Pencil, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { toKstParts } from "@/lib/schedule/format";
import type { Schedule } from "@/types/api/schedule";

const TYPE_LABEL: Record<Schedule["scheduleType"], string> = {
  HEALTH: "건강",
  PERSONAL: "개인",
  WORK: "업무",
  ETC: "기타",
};

/**
 * 컴팩트 아젠다 행 — [완료표시] 시간 | 제목·내용 | 타입 | 수정·삭제.
 * done 은 표시 전용 (PATCH 에 done 필드 없음 — 완료 처리는 챗봇 경로).
 * 액션은 hover(데스크톱) + 행 선택/탭(모바일)으로 노출.
 */
export function ScheduleRow({
  schedule,
  selected,
  disabled,
  onSelect,
  onEdit,
  onDelete,
}: {
  schedule: Schedule;
  selected: boolean;
  disabled: boolean;
  onSelect: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const { time } = toKstParts(schedule.scheduledAt);

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelect();
        }
      }}
      aria-expanded={selected}
      className={cn(
        "group flex cursor-pointer items-center gap-3 border-b border-hairline px-6 py-[10px] outline-none transition-colors focus-visible:bg-surface",
        selected ? "bg-surface" : "hover:bg-surface",
      )}
    >
      <span
        className="flex w-[18px] flex-none items-center justify-center"
        aria-label={schedule.done ? "완료된 일정" : undefined}
      >
        {schedule.done && (
          <span className="flex h-[18px] w-[18px] items-center justify-center rounded-md bg-ink text-white">
            <Check size={13} strokeWidth={3} />
          </span>
        )}
      </span>
      <span
        className={cn(
          "w-[46px] flex-none font-mono text-[13px]",
          schedule.done ? "text-faint" : "text-muted",
        )}
      >
        {time}
      </span>
      <span className="min-w-0 flex-1">
        <span
          className={cn(
            "block truncate text-[15px]",
            schedule.done ? "text-faint line-through" : "text-ink",
          )}
        >
          {schedule.title}
        </span>
        {schedule.content && !schedule.done && (
          <span className="block truncate text-[12px] text-faint">
            {schedule.content}
          </span>
        )}
      </span>
      <span className="flex-none font-mono text-[11px] text-faint">
        {TYPE_LABEL[schedule.scheduleType]}
      </span>
      <span
        className={cn(
          "flex-none items-center gap-3",
          selected ? "flex" : "hidden group-hover:flex",
        )}
      >
        <button
          type="button"
          aria-label="일정 수정"
          disabled={disabled}
          onClick={(e) => {
            e.stopPropagation();
            onEdit();
          }}
          className="text-muted transition-colors hover:text-ink disabled:opacity-40"
        >
          <Pencil size={15} />
        </button>
        <button
          type="button"
          aria-label="일정 삭제"
          disabled={disabled}
          onClick={(e) => {
            e.stopPropagation();
            onDelete();
          }}
          className="text-muted transition-colors hover:text-ink disabled:opacity-40"
        >
          <Trash2 size={15} />
        </button>
      </span>
    </div>
  );
}
