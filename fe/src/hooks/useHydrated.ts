"use client";

import { useEffect, useState } from "react";

/** persist 스토어 하이드레이션 완료 여부. SSR/CSR 불일치 방지. */
export function useHydrated(): boolean {
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => setHydrated(true), []);
  return hydrated;
}
