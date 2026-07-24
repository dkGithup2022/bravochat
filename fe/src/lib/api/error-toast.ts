import { toast } from "sonner";
import { ApiError } from "./types";

/** API 에러 → 토스트. 401/403 은 전역(AuthProvider)에서 처리하므로 조용히 무시. */
export function showErrorToast(error: unknown, fallback: string): void {
  if (error instanceof ApiError) {
    if (error.status === 401 || error.status === 403) return;
    toast.error(error.message || fallback);
    return;
  }
  toast.error(error instanceof Error ? error.message || fallback : fallback);
}
