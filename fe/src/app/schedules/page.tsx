"use client";

import { useRequireAuth } from "@/hooks/useRequireAuth";
import { ScheduleScreen } from "@/components/schedule/ScheduleScreen";

export default function SchedulesPage() {
  const { isReady } = useRequireAuth();

  // 미인증이면 useRequireAuth 가 /login 으로 리다이렉트 (그 사이 빈 화면)
  if (!isReady) {
    return (
      <div className="flex flex-1 items-center justify-center text-faint">
        …
      </div>
    );
  }

  return <ScheduleScreen />;
}
