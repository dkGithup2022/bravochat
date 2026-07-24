"use client";

import { useState } from "react";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";
import { Avatar } from "./Avatar";

/**
 * 일정 체크리스트 카드 (Bravo Message 정본 schedule).
 * 항목 클릭 시 체크 토글. 봇 메시지처럼 아바타 + 좌측 정렬.
 *
 * ⚠️ 현재 BE 는 구조화 응답을 안 준다 → 목/위젯 데모 전용.
 */
export function ScheduleCard({
  items,
  title = "이번주 일정",
}: {
  items: string[];
  title?: string;
}) {
  const [checked, setChecked] = useState<boolean[]>(() => items.map(() => false));

  const toggle = (i: number) =>
    setChecked((prev) => prev.map((c, idx) => (idx === i ? !c : c)));

  return (
    <div className="flex w-full items-start gap-[9px]">
      <Avatar />
      <div className="max-w-[80%] overflow-hidden rounded-[4px_16px_16px_16px] border border-line bg-white">
        <div className="border-b border-line bg-surface px-4 py-[11px] font-mono text-sm text-muted">
          {title}
        </div>
        {items.map((item, i) => (
          <button
            key={i}
            type="button"
            onClick={() => toggle(i)}
            className="flex w-full items-center gap-[11px] border-b border-hairline px-4 py-[13px] text-left text-base text-ink last:border-b-0"
          >
            <span
              className={cn(
                "flex h-[18px] w-[18px] flex-none items-center justify-center rounded-md border-[1.8px]",
                checked[i] ? "border-ink bg-ink text-white" : "border-faint",
              )}
            >
              {checked[i] && <Check size={13} strokeWidth={3} />}
            </span>
            <span className={cn(checked[i] && "text-muted line-through")}>
              {item}
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}
