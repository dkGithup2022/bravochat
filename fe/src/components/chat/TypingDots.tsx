import { Avatar } from "./Avatar";

/** 봇 응답 대기(pending) 인디케이터 — 3점 애니메이션 (Chat 시안). */
export function TypingDots() {
  return (
    <div className="flex w-full items-start gap-[9px]">
      <Avatar />
      <div className="flex gap-[5px] rounded-[4px_16px_16px_16px] border border-line bg-surface px-[18px] py-4">
        <i className="h-[7px] w-[7px] animate-typing rounded-full bg-faint" />
        <i
          className="h-[7px] w-[7px] animate-typing rounded-full bg-faint"
          style={{ animationDelay: "0.2s" }}
        />
        <i
          className="h-[7px] w-[7px] animate-typing rounded-full bg-faint"
          style={{ animationDelay: "0.4s" }}
        />
      </div>
    </div>
  );
}
