"use client";

import { toast } from "sonner";
import { WidgetCard } from "./WidgetCard";
import { WIDGETS } from "@/features/chat/widgets";

/** 마스코트 하단 위젯 2×2 그리드. 활성=onPick(prompt) / 비활성=준비중 토스트. */
export function WidgetGrid({
  onPick,
  disabled = false,
}: {
  onPick: (prompt: string) => void;
  /** 전송 대기/이력 로딩 중 그리드 전체 비활성 */
  disabled?: boolean;
}) {
  return (
    <div className="grid w-full grid-cols-2 gap-[10px]">
      {WIDGETS.map((w) => (
        <WidgetCard
          key={w.id}
          icon={w.icon}
          label={w.label}
          meta={w.meta}
          disabled={disabled || w.disabled}
          onClick={() => {
            if (disabled) return;
            if (w.disabled) toast("위젯은 준비중입니다");
            else onPick(w.prompt);
          }}
        />
      ))}
    </div>
  );
}
