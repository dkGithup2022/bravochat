// k8s liveness/readiness + Docker HEALTHCHECK 용. 인증 없이 200만 반환한다.
export function GET() {
  return new Response("ok", { status: 200 });
}
