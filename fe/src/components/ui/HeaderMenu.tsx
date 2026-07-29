"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { CalendarDays, History, LogOut, Menu, MessageCircle } from "lucide-react";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { href: "/", label: "챗", Icon: MessageCircle },
  { href: "/schedules", label: "일정", Icon: CalendarDays },
  { href: "/records", label: "기록", Icon: History },
] as const;

/**
 * 헤더 우측 통합 메뉴 — 챗/일정 이동 + 로그아웃을 버튼 하나의 드롭다운으로.
 * 바깥 클릭/Escape 로 닫힘. 챗·일정 화면 공용.
 */
export function HeaderMenu({ onLogout }: { onLogout: () => void }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const pathname = usePathname();

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (e: PointerEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label="메뉴"
        aria-expanded={open}
        aria-haspopup="menu"
        className={cn(
          // -my-1: 헤더 높이는 텍스트 기준 유지, 터치 영역만 32px 확보
          "-my-1 flex h-8 w-8 items-center justify-center rounded-field transition-colors",
          open ? "bg-surface text-ink" : "text-muted hover:text-ink",
        )}
      >
        <Menu size={19} />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 top-[calc(100%+8px)] z-30 w-[160px] overflow-hidden rounded-card border border-line bg-white shadow-[0_4px_12px_rgba(0,0,0,0.08)]"
        >
          {NAV_ITEMS.map(({ href, label, Icon }) => {
            const active =
              href === "/" ? pathname === "/" : pathname.startsWith(href);
            return (
              <Link
                key={href}
                href={href}
                role="menuitem"
                aria-current={active ? "page" : undefined}
                onClick={() => setOpen(false)}
                className={cn(
                  "flex items-center gap-[10px] px-4 py-[11px] text-[14px] transition-colors",
                  active
                    ? "bg-surface font-bold text-ink"
                    : "text-muted hover:bg-surface hover:text-ink",
                )}
              >
                <Icon size={15} />
                {label}
              </Link>
            );
          })}
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              onLogout();
            }}
            className="flex w-full items-center gap-[10px] border-t border-hairline px-4 py-[11px] text-left text-[14px] text-muted transition-colors hover:bg-surface hover:text-ink"
          >
            <LogOut size={15} />
            로그아웃
          </button>
        </div>
      )}
    </div>
  );
}
