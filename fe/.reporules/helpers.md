# Helpers

| 함수 | 위치 | 설명 |
|------|------|------|
| `cn(...inputs)` | `src/lib/utils.ts` | clsx + tailwind-merge 클래스 병합 |
| `useHydrated()` | `src/hooks/useHydrated.ts` | persist 하이드레이션 완료 여부 |
| `useRequireAuth()` | `src/hooks/useRequireAuth.ts` | 미인증 시 /login 리다이렉트 |
| `useAuthStore` | `src/store/auth-store.ts` | zustand persist (token, username) |
| `showErrorToast(err, fallback)` | `src/lib/api/error-toast.ts` | 에러 토스트 (401/403 무시) |
