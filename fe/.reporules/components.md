# Components

모두 `src/components/chat/`. Handoff §4b + Bravo Message.dc.html 정본 기준.

| 컴포넌트 | props | 설명 |
|----------|-------|------|
| `Mascot` | className? | 상단 큰 마스코트, bob 애니메이션 |
| `Avatar` | — | 봇 메시지 40px 원형 마스코트 아바타 |
| `MessageBubble` | who('user'\|'bot'), text | user 우측·다크 / bot 좌측·surface+아바타 |
| `TypingDots` | — | 봇 pending 3점 애니메이션 |
| `ScheduleCard` | items[], title? | 체크리스트 카드(토글). 목/데모 전용 |
| `OptionChips` | options[], selected?, onSelect? | 선택지 칩(기본/다크). 목/데모 전용 |
| `ChatMessage` | message(Message), onSelectOption? | who 디스패처 → 위 컴포넌트 |
| `WidgetCard` | icon?, label, meta?, onClick?, disabled? | 위젯 2×2. 기본/호버/미정(점선·회색) |
| `ChatInput` | value, onChange, onSend, onVoice?, disabled?, placeholder? | 텍스트+음성. 값 있으면 Send, 없으면 Mic. Enter 전송 |

아이콘: `lucide-react` (Check, SquareDashed, Mic, Send 등). 색/라운드: 테마 토큰(text-ink, bg-surface, border-line, rounded-bubble/card/field).
