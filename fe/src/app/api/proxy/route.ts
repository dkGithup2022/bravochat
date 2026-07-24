import ky, { HTTPError } from "ky";
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

/**
 * BFF 프록시 — 브라우저 → same-origin(/api/proxy) → 실제 BE(BACKEND_API_URL).
 * 브라우저가 BE 를 직접 호출하지 않으므로 CORS 를 타지 않는다.
 *
 * 인증: 레퍼런스의 Cookie 릴레이를 Bravo 의 Bearer 토큰에 맞게 치환.
 *  - 요청: 브라우저가 보낸 Authorization 헤더를 그대로 BE 로 전달.
 *  - 응답: BE 가 내려준 Authorization 헤더(로그인 토큰)를 캡처해 envelope 로 릴레이.
 */

const ProxyRequestSchema = z.object({
  method: z.enum(["GET", "POST", "PUT", "DELETE"]),
  path: z.string().startsWith("/"),
  body: z.unknown().optional(),
  params: z
    .record(z.string(), z.union([z.string(), z.number(), z.boolean()]))
    .optional(),
});

function getBackendUrl(): string {
  const url = process.env.BACKEND_API_URL;
  if (!url) {
    throw new Error("BACKEND_API_URL environment variable is not set");
  }
  return url;
}

export async function POST(request: NextRequest) {
  try {
    const parsed = ProxyRequestSchema.safeParse(await request.json());
    if (!parsed.success) {
      return NextResponse.json(
        { error: "Invalid request format", status: 400, data: parsed.error.format() },
        { status: 400 },
      );
    }

    const { method, path, body: requestBody, params } = parsed.data;
    // 새 ky: baseUrl 사용. baseUrl 는 트레일링 슬래시, input(cleanPath)은 리딩 슬래시 없이 결합.
    const baseUrl = getBackendUrl().replace(/\/?$/, "/");
    const backendClient = ky.create({
      baseUrl,
      timeout: 30000,
      retry: { limit: 0 },
    });

    if (process.env.NODE_ENV === "development") {
      console.log(`[API Proxy] ${method} ${getBackendUrl()}${path}`);
    }

    const cleanPath = path.startsWith("/") ? path.substring(1) : path;
    const incomingAuth = request.headers.get("authorization");

    const backendResponse = await backendClient(cleanPath, {
      method,
      searchParams: params
        ? Object.fromEntries(
            Object.entries(params).map(([k, v]) => [k, String(v)]),
          )
        : undefined,
      json: requestBody,
      headers: {
        "Content-Type": "application/json",
        ...(incomingAuth ? { Authorization: incomingAuth } : {}),
      },
    });

    // 바디 파싱 (204/빈 응답 대응)
    let responseData: unknown = null;
    const contentLength = backendResponse.headers.get("content-length");
    const contentType = backendResponse.headers.get("content-type");
    if (contentLength !== "0" && contentType?.includes("application/json")) {
      const text = await backendResponse.text();
      responseData = text ? JSON.parse(text) : null;
    }

    // 로그인 토큰: BE 응답 Authorization 헤더 캡처
    const authHeader = backendResponse.headers.get("authorization");

    return NextResponse.json(
      {
        data: responseData,
        status: backendResponse.status,
        ...(authHeader ? { authorization: authHeader } : {}),
      },
      { status: 200 },
    );
  } catch (error) {
    if (error instanceof HTTPError) {
      const errorData = await error.response.json().catch(() => null);
      const rec = errorData as Record<string, string> | null;
      if (process.env.NODE_ENV === "development") {
        console.error(`[API Proxy] HTTPError ${error.response.status}:`, errorData);
      }
      return NextResponse.json(
        {
          error: rec?.message || rec?.error || `Backend request failed (${error.response.status})`,
          status: error.response.status,
          data: errorData,
        },
        { status: error.response.status },
      );
    }

    console.error("[API Proxy] Error:", error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Internal server error", status: 500 },
      { status: 500 },
    );
  }
}
