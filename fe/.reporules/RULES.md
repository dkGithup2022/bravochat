# .reporules 관리 규칙

이 디렉토리는 프로젝트의 현재 상태를 요약한 인덱스다. dk- 스킬들이 읽고 갱신한다.

- `components.md` — 재사용 컴포넌트 목록 (이름, 위치, props 요약)
- `models.md` — 데이터 모델/타입 목록
- `helpers.md` — 유틸/헬퍼 함수 목록
- `routes.md` — App Router 페이지 라우트 목록
- `api-routes.md` — Next route handler(목/프록시) 목록
- `api-specs.md` — 백엔드(be) API 계약 명세

## 갱신 규칙
- 코드 추가/변경 시 해당 문서를 함께 갱신한다.
- `dk-fe-sync-reporules` 로 소스 스캔 후 전체 재생성 가능.
