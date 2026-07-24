"use client";

import { cn } from "@/lib/utils";

/**
 * 선택지 칩 (Bravo Message 정본 options).
 * 기본 흰색 / 선택(다크). 클릭 시 onSelect.
 *
 * ⚠️ 현재 BE 는 구조화 응답을 안 준다 → 목/데모 전용.
 */
export function OptionChips({
  options,
  selected,
  onSelect,
}: {
  options: string[];
  selected?: string;
  onSelect?: (option: string) => void;
}) {
  return (
    <div className="flex max-w-[92%] flex-wrap gap-[9px]">
      {options.map((opt, i) => {
        const isSelected = selected === opt;
        return (
          <button
            key={i}
            type="button"
            onClick={() => onSelect?.(opt)}
            className={cn(
              "rounded-[10px] border px-4 py-[11px] text-base font-semibold transition-colors",
              isSelected
                ? "border-ink bg-ink text-white"
                : "border-line bg-white text-ink hover:bg-surface",
            )}
          >
            {opt}
          </button>
        );
      })}
    </div>
  );
}
