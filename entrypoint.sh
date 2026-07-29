#!/bin/sh
# be(Spring Boot) + fe(Next.js standalone)를 한 컨테이너에서 함께 기동.
# 둘 중 하나라도 죽으면 컨테이너를 종료시켜 k8s가 재시작하도록 한다.

echo "[entrypoint] starting be (spring boot) ..."
java -jar /app/app.jar --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" &
JAVA_PID=$!

echo "[entrypoint] starting fe (next.js standalone) ..."
node /app/fe/server.js &
NODE_PID=$!

shutdown() {
  echo "[entrypoint] shutting down ..."
  kill -TERM "$JAVA_PID" "$NODE_PID" 2>/dev/null
  wait
  exit 0
}
trap shutdown TERM INT

while kill -0 "$JAVA_PID" 2>/dev/null && kill -0 "$NODE_PID" 2>/dev/null; do
  sleep 2
done

echo "[entrypoint] a child process exited unexpectedly, stopping container"
kill -TERM "$JAVA_PID" "$NODE_PID" 2>/dev/null
wait
exit 1
