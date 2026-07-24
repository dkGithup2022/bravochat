import { Avatar } from "./Avatar";

/**
 * 텍스트 말풍선 — Bravo Message 정본.
 * user: 우측·다크(ink) / bot: 좌측·surface + 원형 마스코트 아바타.
 */
export function MessageBubble({
  who,
  text,
}: {
  who: "user" | "bot";
  text: string;
}) {
  if (who === "user") {
    return (
      <div className="flex w-full justify-end">
        <div className="max-w-[80%] rounded-[16px_4px_16px_16px] bg-ink px-4 py-[13px] text-[17px] leading-[1.5] text-white">
          {text}
        </div>
      </div>
    );
  }

  return (
    <div className="flex w-full items-start gap-[9px]">
      <Avatar />
      <div className="max-w-[80%] whitespace-pre-line rounded-[4px_16px_16px_16px] border border-line bg-surface px-4 py-[13px] text-[17px] leading-[1.55] text-ink">
        {text}
      </div>
    </div>
  );
}
