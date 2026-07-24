import type { ReactNode } from "react";
import { SquareDashed } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * 위젯 카드 (마스코트 하단 2×2). 클릭 시 prompt 주입 (Handoff §4).
 * 기본 / 호버(surface+ink 보더) / 미정(disabled: 점선·회색).
 */
export function WidgetCard({
  icon,
  label,
  meta,
  onClick,
  disabled = false,
}: {
  icon?: ReactNode;
  label: string;
  meta?: string;
  onClick?: () => void;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex items-center gap-[10px] rounded-card border px-[14px] py-3 text-left transition-colors",
        disabled
          ? "border-dashed border-line bg-white"
          : "border-line bg-white hover:border-ink hover:bg-surface",
      )}
    >
      <span className={cn("flex-none", disabled ? "text-faint" : "text-ink")}>
        {disabled ? <SquareDashed size={20} strokeWidth={1.8} /> : icon}
      </span>
      <span
        className={cn(
          "min-w-0 flex-1 truncate whitespace-nowrap text-base font-bold",
          disabled ? "text-faint" : "text-ink",
        )}
      >
        {label}
      </span>
      {meta && !disabled && (
        <span className="whitespace-nowrap font-mono text-[13px] text-muted">
          {meta}
        </span>
      )}
    </button>
  );
}
