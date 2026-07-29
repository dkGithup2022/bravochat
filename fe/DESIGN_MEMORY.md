# Design Memory

## Brand Tone
- **Adjectives:** 미니멀, 모노크롬(GitHub 라이트), 친근(마스코트), 정돈됨
- **Avoid:** 유채색 남발 — 브랜드 블루(#1a6dff)는 화면당 한 곳(진행률·포커스 등)만

## Layout & Spacing
- **셸:** 캔버스(`bg-canvas`) 위 중앙 정렬 520px 폰 프레임 (`md:rounded-[36px] border-line` + lg shadow), 모바일 풀스크린 `h-dvh`. 모든 화면 공통.
- **Density:** 화면 성격 따라 — 챗은 comfortable, 일정은 compact 아젠다(사용자 선택)
- **수평 패딩:** 프레임 내부 `px-6` (헤더·본문 정렬 라인 통일)

## Typography
- **본문:** Noto Sans KR (`font-sans`), 제목 15px/보조 12px, 굵기 대비(400 vs 700) 위주
- **숫자·라벨·메타:** Space Mono (`font-mono`) — 시간, 날짜, 건수, 타입 라벨, 헤더 로고

## Color
- 토큰만 사용: ink/muted/faint(텍스트 3단계), line/hairline(보더 2단계), surface(면), canvas(바깥), brand(포인트)
- **타입 구분도 모노크롬:** 유채색 대신 mono 텍스트 라벨(건강/개인/업무/기타) 또는 명도 도트

## Interaction Patterns
- **내비게이션:** 헤더 우측 통합 메뉴 버튼 하나(`HeaderMenu`) — 드롭다운에 챗/일정(active=bold+surface)+로그아웃. 중앙 pill 탭은 유저가 반려("구리다")한 안 — 재사용 금지.
- **완료 표시:** done = 18px `bg-ink` 체크 배지 + 제목 line-through(faint). **표시 전용** — BE PATCH 에 done 필드 없어 FE 토글 불가(완료 처리는 챗봇 경로).
- **행 액션(수정/삭제):** hover(데스크톱) + 행 탭 선택(모바일)으로 노출 — hover 전용 금지
- **리스트 그룹:** sticky 날짜 헤더 (`bg-surface` + mono 12px), 오늘/내일 상대 라벨
- **타임라인(기록):** 좌측 `border-l line` + 날짜 도트(11px, border-ink), 최신순. 긴 본문은 `line-clamp-2` + 탭 펼침(`aria-expanded`). 미래 데이터(일정)=오름차순, 과거 이력(기록)=최신순.
- **요약 바:** 헤더 바로 아래 flex-none 줄 — 좌 mono 카운트/기간, 우 액션 또는 정렬 표시. 목록형 화면 공통 자리.
- **로딩/에러/빈 상태:** ChatScreen 문구 톤 재사용 (`…하는 중` / `다시 시도` 버튼 / 안내+유도 2줄)
- **피드백:** sonner 토스트 (`showErrorToast` 헬퍼)

## Accessibility Rules
- 클릭 요소는 전부 `<button>` + aria-label, focus-visible 링
- 파괴적 액션(삭제)은 확인 단계 필수

## Repo Conventions
- **컴포넌트:** `src/components/<domain>/` PascalCase, 화면 조립은 `<Domain>Screen`/페이지, 상태 훅은 `src/features/<domain>/use<X>.ts`
- **스타일:** Tailwind v4 유틸 + globals.css `@theme` 시맨틱 토큰 (`text-ink` 등). 인라인 hex 금지.
- **radius:** bubble 16 / card 12 / field 14 (토큰)

---

*Updated by design-lab — 2026-07-29 SchedulesPage (Variant C)*
