import { env } from "@/env.mjs";

/** NEXT_PUBLIC_USE_MOCK=true 이면 목 데이터 사용, false 면 실제 BE 프록시. */
export function isMockEnabled(): boolean {
  return env.NEXT_PUBLIC_USE_MOCK === true;
}
