import type { ProxyRequest, ProxyResponse } from "../types";
import type { RecentTurn } from "@/types/api/chat";

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

  return fail(404, `Mock 미구현 엔드포인트: ${key}`) as ProxyResponse<T>;
}
