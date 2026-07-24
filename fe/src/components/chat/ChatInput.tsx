"use client";

import { Mic, Send } from "lucide-react";

/**
 * 입력바 (Handoff §4). 텍스트 + 음성 두 진입.
 * 값이 있으면 전송(Send), 비어 있으면 음성(Mic). Enter 전송. 56px 전송 버튼.
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
  const hasText = value.trim().length > 0;

  const handleAction = () => {
    if (disabled) return;
    if (hasText) onSend();
    else onVoice?.();
  };

  return (
    <div className="flex items-center gap-[10px]">
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && !e.nativeEvent.isComposing) {
            e.preventDefault();
            if (hasText && !disabled) onSend();
          }
        }}
        placeholder={placeholder}
        disabled={disabled}
        className="min-w-0 flex-1 rounded-field border border-line bg-surface px-5 py-[15px] text-base text-ink outline-none placeholder:text-faint focus:border-ink disabled:opacity-60"
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
  );
}
