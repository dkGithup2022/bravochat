import type { RecordsResponse } from "@/types/api/record";
import { mockGetRecords, mockDeleteRecord } from "./mock/records";

/**
 * 기록 API — ⚠️ BE 미구현.
 * USE_MOCK 환경과 무관하게 **항상 인프로세스 목**을 사용한다 (실 환경에서도 목 데이터 표시).
 * BE 계약 확정 시 이 파일만 apiGet/apiDelete 호출로 교체하면 된다 (시그니처 유지).
 */

export function getRecords(): Promise<RecordsResponse> {
  return mockGetRecords().then((records) => ({ records }));
}

export function deleteRecord(recordId: number): Promise<void> {
  return mockDeleteRecord(recordId);
}
