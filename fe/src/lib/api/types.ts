export interface ProxyRequest {
  method: "GET" | "POST" | "PATCH" | "DELETE";
  path: string;
  body?: unknown;
  params?: Record<string, string | number | boolean>;
  options?: {
    /** 인증 헤더를 붙이지 않음 (로그인 등 공개 엔드포인트) */
    skipAuth?: boolean;
  };
}

export interface ProxyResponse<T = unknown> {
  data?: T;
  error?: string;
  status: number;
  /**
   * BE 가 응답 헤더로 내려준 Authorization 값 (로그인 시 토큰 발급).
   * 프록시가 캡처해서 릴레이. Bearer 접두어 포함.
   */
  authorization?: string;
}

/** 정규화된 API 에러. status 로 401(세션)·400(검증)·500(LLM) 등 분기. */
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public data?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
