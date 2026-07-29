import { Calendar, NotebookText } from "lucide-react";
import type { ReactNode } from "react";

/** 마스코트 하단 위젯 2×2. 클릭 시 prompt 를 유저 메시지로 주입 (Chat 시안). */
export interface WidgetDef {
  id: string;
  icon?: ReactNode;
  label: string;
  meta?: string;
  prompt: string;
  disabled?: boolean;
}

export const WIDGETS: WidgetDef[] = [
  {
    id: "schedule",
    icon: <Calendar size={20} strokeWidth={1.8} />,
    label: "이번주 일정",
    prompt: "이번주 일정 보여줘",
  },
  {
    id: "journal",
    icon: <NotebookText size={20} strokeWidth={1.8} />,
    label: "일상 기록",
    prompt: "일상 기록 보여줘",
  },
  // 미정 자리표시자 (Chat 시안: 점선·회색)
  { id: "widget1", label: "준비중", prompt: "", disabled: true },
  { id: "widget2", label: "준비중", prompt: "", disabled: true },
];
