import type { Message } from "@/types/data/chat";
import { MessageBubble } from "./MessageBubble";
import { ScheduleCard } from "./ScheduleCard";
import { OptionChips } from "./OptionChips";

/** who 에 따라 알맞은 메시지 컴포넌트로 디스패치 (Bravo Message 정본). */
export function ChatMessage({
  message,
  onSelectOption,
}: {
  message: Message;
  onSelectOption?: (option: string) => void;
}) {
  switch (message.who) {
    case "user":
      return <MessageBubble who="user" text={message.text} />;
    case "bot":
      return <MessageBubble who="bot" text={message.text} />;
    case "schedule":
      return <ScheduleCard items={message.items} />;
    case "options":
      return <OptionChips options={message.options} onSelect={onSelectOption} />;
  }
}
