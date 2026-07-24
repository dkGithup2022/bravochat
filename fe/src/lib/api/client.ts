import ky, { HTTPError } from "ky";
import { z } from "zod";
import { ApiError } from "./types";
import type { ProxyRequest, ProxyResponse } from "./types";
import { handleMockRequest } from "./mock";
import { isMockEnabled } from "./mock/enabled";
import { useAuthStore } from "@/store/auth-store";

const API_PROXY_URL = "/api/proxy";

const proxyClient = ky.create({
  timeout: 35000,
  retry: {
    limit: 2,
    methods: ["get"],
    statusCodes: [408, 429, 500, 502, 503, 504],
  },
});

const EnvelopeSchema = z.object({
  data: z.unknown().optional(),
  error: z.string().optional(),
  status: z.number(),
  authorization: z.string().optional(),
});

/** 401/403 → 전역 세션 만료 이벤트. AuthProvider 가 듣고 logout + 리다이렉트. */
function dispatchUnauthorized(status: number, message: string): void {
  if (typeof window === "undefined") return;
  window.dispatchEvent(
    new CustomEvent("auth:unauthorized", { detail: { status, message } }),
  );
}

/** mock 또는 same-origin 프록시로 요청을 보내고 정규화된 envelope 반환. */
async function dispatch(request: ProxyRequest): Promise<ProxyResponse> {
  // 목 모드
  if (isMockEnabled()) {
    return handleMockRequest(request);
  }

  if (typeof window === "undefined") {
    throw new ApiError(
      `API 는 브라우저(CSR)에서만 호출하세요. path: ${request.path}`,
      0,
    );
  }

  // 실제: same-origin 프록시로 위임 (CORS 회피). Bearer 토큰 부착.
  const token = request.options?.skipAuth
    ? undefined
    : useAuthStore.getState().token;

  try {
    const raw = await proxyClient
      .post(API_PROXY_URL, {
        json: request,
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      })
      .json<unknown>();

    const parsed = EnvelopeSchema.safeParse(raw);
    if (!parsed.success) {
      return { error: "Invalid response format from server", status: 500 };
    }
    return parsed.data as ProxyResponse;
  } catch (error) {
    // 프록시가 백엔드 에러 상태를 그대로 전달 → HTTPError 로 도착
    if (error instanceof HTTPError) {
      const body = await error.response.json().catch(() => null);
      const rec = body as Record<string, string> | null;
      return {
        error: rec?.error || rec?.message || error.message,
        status: error.response.status,
        data: body,
      };
    }
    return {
      error: error instanceof Error ? error.message : "Unknown error",
      status: 0,
    };
  }
}

/** envelope 를 data 로 언랩. 에러면 ApiError throw (401 은 전역 이벤트도 발화). */
function unwrap<T>(env: ProxyResponse): T {
  if (env.error) {
    if (env.status === 401 || env.status === 403) {
      dispatchUnauthorized(env.status, env.error);
    }
    throw new ApiError(env.error, env.status, env.data);
  }
  return env.data as T;
}

export async function apiGet<T>(
  path: string,
  params?: Record<string, string | number | boolean>,
  options?: { skipAuth?: boolean },
): Promise<T> {
  return unwrap<T>(await dispatch({ method: "GET", path, params, options }));
}

export async function apiPost<T>(
  path: string,
  body?: unknown,
  options?: { skipAuth?: boolean },
): Promise<T> {
  return unwrap<T>(await dispatch({ method: "POST", path, body, options }));
}

export async function apiDelete<T>(
  path: string,
  options?: { skipAuth?: boolean },
): Promise<T> {
  return unwrap<T>(await dispatch({ method: "DELETE", path, options }));
}

/**
 * 로그인 전용 — 응답 바디가 아니라 Authorization 헤더로 토큰이 온다(§2-1).
 * envelope.authorization 에서 토큰을 추출해 반환.
 */
export async function apiLogin(body: {
  username: string;
  password: string;
}): Promise<string> {
  const env = await dispatch({
    method: "POST",
    path: "/auth/login",
    body,
    options: { skipAuth: true },
  });
  if (env.error) {
    throw new ApiError(env.error, env.status, env.data);
  }
  const token = env.authorization?.replace(/^Bearer\s+/i, "").trim();
  if (!token) {
    throw new ApiError("로그인 응답에서 토큰을 찾지 못했습니다", 500);
  }
  return token;
}
