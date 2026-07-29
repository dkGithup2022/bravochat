"use client";

import { useSyncExternalStore } from "react";

const emptySubscribe = () => () => {};

/** persist 스토어 하이드레이션 완료 여부. SSR/CSR 불일치 방지 — 서버 false, 클라이언트 true. */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false,
  );
}
