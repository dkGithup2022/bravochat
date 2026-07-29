# fe(Next.js) + be(Spring Boot)를 한 이미지로 묶는 멀티스테이지 빌드.
# 런타임은 entrypoint.sh가 두 프로세스를 함께 기동하고, fe의 BFF 프록시가
# 컨테이너 내부 127.0.0.1:8080 으로 be를 호출한다.

# --- be builder ---
FROM eclipse-temurin:21-jdk AS be-builder
WORKDIR /build
COPY be/ .
RUN ./gradlew :modules:applications:api-application:bootJar --no-daemon

# --- fe deps ---
# node 22: ky@2.x가 node >=22 요구
FROM node:22-alpine AS fe-deps
WORKDIR /app
COPY fe/package.json fe/package-lock.json ./
RUN npm ci

# --- fe builder ---
FROM node:22-alpine AS fe-builder
WORKDIR /app
COPY --from=fe-deps /app/node_modules ./node_modules
COPY fe/ .

ARG NEXT_PUBLIC_USE_MOCK="false"
ENV NEXT_PUBLIC_USE_MOCK=$NEXT_PUBLIC_USE_MOCK

RUN npm run build

# --- runner ---
FROM eclipse-temurin:21-jre-alpine AS runner
RUN apk add --no-cache nodejs

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
ENV BACKEND_API_URL="http://127.0.0.1:8080"

COPY --from=be-builder /build/modules/applications/api-application/build/libs/*.jar /app/app.jar
COPY --from=fe-builder /app/public ./fe/public
COPY --from=fe-builder /app/.next/standalone ./fe/
COPY --from=fe-builder /app/.next/static ./fe/.next/static
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# fe 3000 (외부 진입점) / be 8080 (컨테이너 내부용)
EXPOSE 3000 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --spider -q http://127.0.0.1:3000/healthz || exit 1

ENTRYPOINT ["/entrypoint.sh"]
