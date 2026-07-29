import type { ChatRecord } from "@/types/api/record";
import { addDays, todayKst } from "@/lib/schedule/format";

/**
 * 기록 목 저장소 — BE 기록 API 미구현이라 records 는 환경(USE_MOCK)과 무관하게
 * 항상 이 인프로세스 목만 사용한다 (lib/api/record.ts 참고). BE 확정 시 제거.
 */

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const today = todayKst();
const d = (offset: number) => addDays(today, offset);

let mockRecords: ChatRecord[] = [
  {
    recordId: 21,
    date: d(0),
    summary: "강남 미팅 준비로 발표 자료를 오전까지 마무리하기로 함.",
    recordType: "B",
  },
  {
    recordId: 20,
    date: d(0),
    summary:
      "아침 러닝을 다시 시작했다. 무릎 통증이 있어서 당분간 5km 이하로 유지하고, 통증이 계속되면 병원에 가보기로 함.",
    recordType: "A",
  },
  {
    recordId: 19,
    date: d(-1),
    summary: "주말 가족 모임 장소를 부모님 댁으로 확정. 치킨은 미리 주문하기로.",
    recordType: "C",
  },
  {
    recordId: 17,
    date: d(-3),
    summary: "최근 잠이 얕아졌다고 느낌. 자기 전 폰 사용을 줄여보기로 함.",
    recordType: "A",
  },
  {
    recordId: 16,
    date: d(-5),
    summary:
      "공과금 자동이체 신청 방법을 알아봄. 다음 달부터는 직접 납부하지 않아도 됨.",
    recordType: "B",
  },
  {
    recordId: 15,
    date: d(-6),
    summary: "치과 정기검진 예약 (7/30 10:30). 스케일링 포함.",
    recordType: "A",
  },
];

/** 최신순 (date desc, 동일 일자는 id desc). */
export async function mockGetRecords(): Promise<ChatRecord[]> {
  await sleep(300);
  return [...mockRecords].sort(
    (a, b) => b.date.localeCompare(a.date) || b.recordId - a.recordId,
  );
}

/** 없는 id 는 404 대응 없이 조용히 무시 (목 전용 단순화). */
export async function mockDeleteRecord(recordId: number): Promise<void> {
  await sleep(200);
  mockRecords = mockRecords.filter((r) => r.recordId !== recordId);
}
