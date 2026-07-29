import type { ProxyRequest, ProxyResponse } from "../types";
import type { RecentTurn } from "@/types/api/chat";
import type { Schedule, ScheduleType } from "@/types/api/schedule";
import { addDays, toKstParts, todayKst } from "@/lib/schedule/format";

/**
 * Bravo 목 핸들러 — BE 계약(specs/api-handoff.md)을 인프로세스로 재현.
 * 에러 포맷/상태코드/지연까지 흉내내서 실제 연동 전 UX 검증.
 */

const SEED_USERNAME = "tester";
const SEED_PASSWORD = "password1234";

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function now(): string {
  return new Date().toISOString();
}

// 세션 동안 유지되는 목 대화 이력 (오래된→최신)
let turnSeq = 3;
const mockTurns: RecentTurn[] = [
  {
    turnId: 1,
    userMessage: "안녕",
    assistantMessage: "안녕하세요! 무엇을 도와드릴까요?",
    createdAt: "2026-07-24T04:40:00.000Z",
  },
  {
    turnId: 2,
    userMessage: "이번주 일정 말해줄래?",
    assistantMessage:
      "이번주에는 공과금 내기, 주말 가족 모임, 이마트 다녀오기가 있어요.",
    createdAt: "2026-07-24T04:41:00.000Z",
  },
  {
    turnId: 3,
    userMessage: "고마워",
    assistantMessage: "천만에요. 더 도와드릴 일이 있으면 말씀해 주세요.",
    createdAt: "2026-07-24T04:41:30.000Z",
  },
];

// === 목 일정 저장소 (§2-5~2-8) ===

const SCHEDULE_TYPES: ScheduleType[] = ["HEALTH", "PERSONAL", "WORK", "ETC"];

/** 오늘(KST) + offsetDays 의 KST hour:minute → UTC ISO. */
function kstInstant(offsetDays: number, hour: number, minute = 0): string {
  const dateKey = addDays(todayKst(), offsetDays);
  return new Date(
    `${dateKey}T${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:00+09:00`,
  ).toISOString();
}

let scheduleSeq = 100;
let mockSchedules: Schedule[] = [
  {
    scheduleId: ++scheduleSeq,
    title: "아침 러닝 5km",
    content: "한강공원",
    scheduleType: "HEALTH",
    scheduledAt: kstInstant(0, 7, 0),
    done: true,
  },
  {
    scheduleId: ++scheduleSeq,
    title: "강남 미팅",
    content: "강남역 2번 출구",
    scheduleType: "WORK",
    scheduledAt: kstInstant(0, 15, 0),
    done: false,
  },
  {
    scheduleId: ++scheduleSeq,
    title: "공과금 납부",
    content: null,
    scheduleType: "PERSONAL",
    scheduledAt: kstInstant(0, 18, 0),
    done: false,
  },
  {
    scheduleId: ++scheduleSeq,
    title: "치과 정기검진",
    content: "스케일링 예약",
    scheduleType: "HEALTH",
    scheduledAt: kstInstant(1, 10, 30),
    done: false,
  },
  {
    scheduleId: ++scheduleSeq,
    title: "돌돌이 미팅",
    content: null,
    scheduleType: "ETC",
    scheduledAt: kstInstant(1, 17, 0),
    done: false,
  },
  {
    scheduleId: ++scheduleSeq,
    title: "주간 보고서 제출",
    content: "금요일 오전 마감",
    scheduleType: "WORK",
    scheduledAt: kstInstant(2, 11, 0),
    done: false,
  },
  {
    scheduleId: ++scheduleSeq,
    title: "이마트 장보기",
    content: "주말 가족 모임 준비",
    scheduleType: "PERSONAL",
    scheduledAt: kstInstant(3, 14, 0),
    done: false,
  },
  {
    scheduleId: ++scheduleSeq,
    title: "가족 모임",
    content: "부모님 댁",
    scheduleType: "PERSONAL",
    scheduledAt: kstInstant(4, 12, 0),
    done: false,
  },
];

/** BE ScheduleType.fromOrEtc 재현 — 이외 값/생략은 ETC 흡수. */
function typeOrEtc(value: unknown): ScheduleType {
  return SCHEDULE_TYPES.includes(value as ScheduleType)
    ? (value as ScheduleType)
    : "ETC";
}

/** 아주 단순한 목 봇 응답 생성 (plain text). */
function mockBotReply(message: string): string {
  const text = message.trim();
  if (text.includes("일정")) {
    return "이번주 일정은 공과금 내기, 주말 가족 모임, 이마트 다녀오기예요. 무엇부터 도와드릴까요?";
  }
  if (text.includes("기록")) {
    return "최근 기록으로 3개가 남아 있어요. 자세히 보시겠어요?";
  }
  if (text.includes("안녕") || text.includes("하이")) {
    return "안녕하세요! 오늘 무엇을 도와드릴까요?";
  }
  return `"${text}" 라고 말씀하셨네요. 무엇을 도와드릴까요?`;
}

function ok<T>(data: T): ProxyResponse<T> {
  return { data, status: 200 };
}

function fail(status: number, error: string): ProxyResponse {
  return { error, status };
}

export async function handleMockRequest<T = unknown>(
  request: ProxyRequest,
): Promise<ProxyResponse<T>> {
  const { method, path, body, params } = request;
  const key = `${method} ${path}`;

  // POST /auth/login → 204 + Authorization 헤더
  if (key === "POST /auth/login") {
    const { username, password } = (body ?? {}) as {
      username?: string;
      password?: string;
    };
    if (!username || !password) {
      return fail(400, "message: 아이디와 비밀번호는 필수입니다") as ProxyResponse<T>;
    }
    await sleep(400);
    if (username === SEED_USERNAME && password === SEED_PASSWORD) {
      return {
        data: null,
        status: 200,
        authorization: `Bearer mock-${Date.now()}`,
      } as ProxyResponse<T>;
    }
    return fail(401, "아이디 또는 비밀번호가 올바르지 않습니다") as ProxyResponse<T>;
  }

  // DELETE /auth/session → 204 (멱등)
  if (key === "DELETE /auth/session") {
    return ok(null) as ProxyResponse<T>;
  }

  // POST /chat/turns → 동기 응답 (LLM 지연 흉내)
  if (key === "POST /chat/turns") {
    const { message } = (body ?? {}) as { message?: string };
    if (!message || !message.trim()) {
      return fail(400, "message: 사용자 입력 텍스트는 필수입니다") as ProxyResponse<T>;
    }
    if (message.length > 4000) {
      return fail(400, "message: 4000자를 초과할 수 없습니다") as ProxyResponse<T>;
    }
    await sleep(700 + Math.floor(Math.random() * 800));
    const reply = mockBotReply(message);
    const turnId = ++turnSeq;
    const createdAt = now();
    mockTurns.push({
      turnId,
      userMessage: message,
      assistantMessage: reply,
      createdAt,
    });
    return ok({ turnId, message: reply, createdAt }) as ProxyResponse<T>;
  }

  // GET /chat/turns/recent?size=20 → 오래된→최신
  if (key === "GET /chat/turns/recent") {
    const size = Number(params?.size ?? 20);
    if (!Number.isInteger(size) || size < 1 || size > 20) {
      return fail(400, "size 는 1~20 사이여야 합니다") as ProxyResponse<T>;
    }
    await sleep(300);
    return ok({ turns: mockTurns.slice(-size) }) as ProxyResponse<T>;
  }

  // GET /chat/turns/transcript → [디버그] 전체 전문 (text/plain 문자열 통짜 흉내)
  if (key === "GET /chat/turns/transcript") {
    await sleep(200);
    if (mockTurns.length === 0) {
      return ok("(no turns)") as ProxyResponse<T>;
    }
    const transcript = mockTurns
      .map((t) =>
        [
          `=== turn ${t.turnId} [COMPLETED] ===`,
          `${t.createdAt} USER_MESSAGE: ${t.userMessage}`,
          `${t.createdAt} ASSISTANT_MESSAGE: ${t.assistantMessage}`,
        ].join("\n"),
      )
      .join("\n");
    return ok(transcript) as ProxyResponse<T>;
  }

  // GET /schedules?from&to&size → 기간 내 최신순 (§2-5)
  if (key === "GET /schedules") {
    const from =
      typeof params?.from === "string" && params.from ? params.from : todayKst();
    const to =
      typeof params?.to === "string" && params.to ? params.to : addDays(from, 6);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(from) || !/^\d{4}-\d{2}-\d{2}$/.test(to)) {
      return fail(400, "from/to 는 YYYY-MM-DD 형식이어야 합니다") as ProxyResponse<T>;
    }
    // size 1~100 밖은 에러가 아니라 서버가 범위로 보정 (§2-5)
    const size = Math.min(100, Math.max(1, Math.trunc(Number(params?.size ?? 20)) || 20));
    await sleep(300);
    const schedules = mockSchedules
      .filter((s) => {
        const { dateKey } = toKstParts(s.scheduledAt);
        return dateKey >= from && dateKey <= to;
      })
      .sort((a, b) => b.scheduledAt.localeCompare(a.scheduledAt))
      .slice(0, size);
    return ok({ schedules }) as ProxyResponse<T>;
  }

  // POST /schedules → 201 생성 (§2-6)
  if (key === "POST /schedules") {
    const { title, content, scheduleType, scheduledAt } = (body ?? {}) as {
      title?: string;
      content?: string | null;
      scheduleType?: string;
      scheduledAt?: string;
    };
    if (!title || !title.trim()) {
      return fail(400, "title: 제목은 필수입니다") as ProxyResponse<T>;
    }
    if (title.length > 200) {
      return fail(400, "title: 200자를 초과할 수 없습니다") as ProxyResponse<T>;
    }
    if (!scheduledAt) {
      return fail(400, "scheduledAt: 일정 시각은 필수입니다") as ProxyResponse<T>;
    }
    await sleep(300);
    const created: Schedule = {
      scheduleId: ++scheduleSeq,
      title: title.trim(),
      content: content?.trim() || null,
      scheduleType: typeOrEtc(scheduleType),
      scheduledAt,
      done: false,
    };
    mockSchedules.push(created);
    return ok(created) as ProxyResponse<T>;
  }

  // PATCH /schedules/{id} → 교체 방식: 새 scheduleId 발급 (§2-7)
  const patchMatch = path.match(/^\/schedules\/(\d+)$/);
  if (method === "PATCH" && patchMatch) {
    const id = Number(patchMatch[1]);
    const existing = mockSchedules.find((s) => s.scheduleId === id);
    if (!existing) {
      return fail(404, "일정을 찾을 수 없습니다") as ProxyResponse<T>;
    }
    const { title, content, scheduleType, scheduledAt } = (body ?? {}) as {
      title?: string;
      content?: string | null;
      scheduleType?: string;
      scheduledAt?: string;
    };
    if (title !== undefined && (!title.trim() || title.length > 200)) {
      return fail(400, "title: 1~200자여야 합니다") as ProxyResponse<T>;
    }
    await sleep(300);
    const replaced: Schedule = {
      scheduleId: ++scheduleSeq, // 새 row + 기존 삭제 → ID 가 바뀐다
      title: title !== undefined ? title.trim() : existing.title,
      content: content !== undefined ? content?.trim() || null : existing.content,
      scheduleType:
        scheduleType !== undefined ? typeOrEtc(scheduleType) : existing.scheduleType,
      scheduledAt: scheduledAt ?? existing.scheduledAt,
      done: existing.done,
    };
    mockSchedules = mockSchedules.filter((s) => s.scheduleId !== id);
    mockSchedules.push(replaced);
    return ok(replaced) as ProxyResponse<T>;
  }

  // DELETE /schedules/{id} → 204 (§2-8)
  const deleteMatch = path.match(/^\/schedules\/(\d+)$/);
  if (method === "DELETE" && deleteMatch) {
    const id = Number(deleteMatch[1]);
    if (!mockSchedules.some((s) => s.scheduleId === id)) {
      return fail(404, "일정을 찾을 수 없습니다") as ProxyResponse<T>;
    }
    await sleep(200);
    mockSchedules = mockSchedules.filter((s) => s.scheduleId !== id);
    return ok(null) as ProxyResponse<T>;
  }

  return fail(404, `Mock 미구현 엔드포인트: ${key}`) as ProxyResponse<T>;
}
