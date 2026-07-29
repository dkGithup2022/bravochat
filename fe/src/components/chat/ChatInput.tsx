"use client";

import { useEffect, useRef } from "react";
import { Mic, Send } from "lucide-react";
import { cn } from "@/lib/utils";

const MAX_LENGTH = 4000; // 계약 §2-3: message 최대 4000자
const WARN_LENGTH = 2000; // 이 길이를 넘으면 남은 글자 안내 표시
const MAX_HEIGHT = 140; // 약 5줄, 넘으면 내부 스크롤

/**
 * 입력바 (Handoff §4). 텍스트 + 음성 두 진입.
 * 값이 있으면 전송(Send), 비어 있으면 음성(Mic). 56px 전송 버튼.
 * 여러 줄 입력: 자동 높이 textarea. Enter 전송 / Shift+Enter 줄바꿈.
 */
export function ChatInput({
  value,
  onChange,
  onSend,
  onVoice,
  disabled = false,
  placeholder = "메시지 입력…",
}: {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  onVoice?: () => void;
  disabled?: boolean;
  placeholder?: string;
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const hasText = value.trim().length > 0;

  // 내용에 맞춰 높이 자동 조절 (draft 복원 등 외부 변경도 커버)
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    // scrollHeight 는 border 미포함(box-sizing: border-box) — 2px 보정해 불필요한 스크롤바 방지
    el.style.height = `${Math.min(el.scrollHeight + 2, MAX_HEIGHT)}px`;
  }, [value]);

  const handleAction = () => {
    if (disabled) return;
    if (hasText) onSend();
    else onVoice?.();
  };

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-end gap-[10px]">
        <textarea
          ref={textareaRef}
          rows={1}
          value={value}
          maxLength={MAX_LENGTH}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={(e) => {
            // 한글 IME 조합 커밋 Enter(keyCode 229) 이중 전송 방지
            if (
              e.key === "Enter" &&
              !e.shiftKey &&
              !e.nativeEvent.isComposing &&
              e.keyCode !== 229
            ) {
              e.preventDefault();
              if (hasText && !disabled) onSend();
            }
          }}
          placeholder={placeholder}
          disabled={disabled}
          className="min-w-0 flex-1 resize-none overflow-y-auto rounded-field border border-line bg-surface px-5 py-[15px] text-base leading-[1.4] text-ink outline-none placeholder:text-faint focus:border-ink disabled:opacity-60"
          aria-label="메시지 입력"
        />
        <button
          type="button"
          onClick={handleAction}
          disabled={disabled}
          aria-label={hasText ? "전송" : "음성 입력"}
          className="flex h-14 w-14 flex-none items-center justify-center rounded-field bg-ink text-white transition-opacity disabled:opacity-50"
        >
          {hasText ? (
            <Send size={24} strokeWidth={2} />
          ) : (
            <Mic size={24} strokeWidth={2} />
          )}
        </button>
      </div>
      {value.length > WARN_LENGTH && (
        <p
          className={cn(
            "px-1 text-right text-[12px]",
            value.length >= MAX_LENGTH ? "text-red-600" : "text-faint",
          )}
        >
          {value.length >= MAX_LENGTH
            ? "4,000자 이상은 쓸 수 없어요"
            : `4,000자까지 입력할 수 있어요 (${value.length.toLocaleString()} / 4,000)`}
        </p>
      )}
    </div>
  );
}
