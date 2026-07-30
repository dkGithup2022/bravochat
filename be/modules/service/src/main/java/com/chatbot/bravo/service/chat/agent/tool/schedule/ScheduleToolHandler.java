package com.chatbot.bravo.service.chat.agent.tool.schedule;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import com.chatbot.bravo.service.chat.agent.tool.AbstractToolHandler;
import com.chatbot.bravo.service.chat.agent.tool.ToolContext;
import com.chatbot.bravo.service.chat.agent.tool.ToolResponse;
import com.chatbot.bravo.service.schedule.ScheduleWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * schedule 툴 — 일정 등록(add)/조회(list)/변경(update→apply_update 투스텝).
 * 블랙박스: 외부는 name/description만 안다.
 * 오퍼레이션 선택·인자 해석은 추출 콜(paramSpec)이, 검증·시간 변환·조회 정책은 여기가 소유.
 *
 * <p>변경은 대화형 투스텝: update(조회+확인 제안, DB 무변경) → 유저 동의 →
 * apply_update(새 row 추가 + 기존 row soft delete). 유저에게 내부 ID를 노출하지 않고
 * 제목+시각으로 대상을 특정한다 — 확인 메시지의 표기가 2단계 추출의 근거가 된다.
 */
@Component
public class ScheduleToolHandler extends AbstractToolHandler<ScheduleParams> {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int LIST_DEFAULT_DAYS = 7;
    private static final int LIST_CAP = 20;

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd(E) HH:mm", Locale.KOREAN).withZone(KST);

    private static final String PARAM_SPEC = """
            # schedule 툴 파라미터 스펙

            대화의 마지막 사용자 요청이 일정과 관련해 무엇을 원하는지 판단해 op를 정하세요.

            - 일정 등록 요청이면: {"op":"add","title":"<제목>","scheduledAt":"YYYY-MM-DDTHH:mm",
              "scheduleType":"HEALTH|PERSONAL|WORK|ETC","content":"<상세, 없으면 생략>"}
              * scheduledAt은 한국시간(Asia/Seoul) 기준. 시각 언급이 없으면 00:00으로 등록
                (예: "내일 등록해줘" → 내일 00:00). 날짜조차 알 수 없을 때만 op=missing으로.
            - 일정 조회 요청이면: {"op":"list","from":"YYYY-MM-DD","to":"YYYY-MM-DD"}
              * to는 포함(inclusive) — 그 날짜의 일정까지 조회된다. 하루 조회면 from=to.
              * 기간 언급이 없으면 from/to 생략 (기본: 오늘부터 7일).
            - 일정 변경 요청이면(1단계 — 아직 유저 확인 전):
              {"op":"update","targetTitle":"<바꿀 일정의 제목>","targetDate":"YYYY-MM-DD",
               "scheduledAt":"YYYY-MM-DDTHH:mm","title":"<새 제목>","scheduleType":"...","content":"..."}
              * targetTitle은 필수. targetDate는 대상 일정의 날짜 — 대화로 알 수 없으면 생략.
              * 변경할 값만 채우세요 (시간만 바꾸면 scheduledAt만).
              * 변경은 한 번에 1건만 — 여러 건 변경 요청이면 그중 첫 한 건만 target으로 정하세요.
            - 직전 어시스턴트 메시지가 일정 변경 확인을 요청했고 유저가 방금 동의했으면(2단계):
              {"op":"apply_update","targetTitle":"<대상 제목>","targetScheduledAt":"YYYY-MM-DDTHH:mm",
               "scheduledAt":"YYYY-MM-DDTHH:mm","title":"...","scheduleType":"...","content":"..."}
              * targetScheduledAt은 변경 전 시각 — 확인 메시지에 있던 값을 그대로. 변경할 값만 채우세요.
            - 등록/변경에 필요한 정보가 대화에 없으면:
              {"op":"missing","question":"<유저에게 물을 한 문장>"}""";

    private final ScheduleRepository scheduleRepository;   // 조회(후보 검색·list) — 프레젠테이션 정책은 툴 소유
    private final ScheduleWriter scheduleWriter;           // 저장(등록·교체) — 쓰기 정책은 usecase 소유

    public ScheduleToolHandler(ToolParamExtractor paramExtractor, ObjectMapper objectMapper,
                               ScheduleRepository scheduleRepository, ScheduleWriter scheduleWriter) {
        super(paramExtractor, objectMapper);
        this.scheduleRepository = scheduleRepository;
        this.scheduleWriter = scheduleWriter;
    }

    @Override public String name() { return "schedule"; }

    @Override public String promptText() {
        return "유저의 일정을 관리한다 — 등록(예: \"내일 3시 회의 잡아줘\"), 조회(예: \"이번 주 일정 뭐 있지?\"), "
                + "변경(예: \"회의 4시로 바꿔줘\" — 변경은 대상을 확인받은 뒤 적용된다)";
    }

    @Override protected String paramSpec() { return PARAM_SPEC; }

    @Override protected Class<ScheduleParams> paramType() { return ScheduleParams.class; }

    @Override
    protected ToolResponse doToolLogic(ScheduleParams params, ToolContext ctx) {
        return switch (params.op() == null ? "" : params.op()) {
            case "add" -> add(params, ctx);
            case "list" -> list(params, ctx);
            case "update" -> update(params, ctx);
            case "apply_update" -> applyUpdate(params, ctx);
            case "missing" -> ToolResponse.fail(params.question() != null
                    ? params.question() : "등록에 필요한 정보가 부족합니다");
            default -> ToolResponse.fail("unknown schedule op: " + params.op());
        };
    }

    // ------------------------------------------------------------------ add

    private ToolResponse add(ScheduleParams params, ToolContext ctx) {
        String title = params.title();
        if (title == null || title.isBlank()) {
            return ToolResponse.fail("title is required");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return ToolResponse.fail("title too long (max " + MAX_TITLE_LENGTH + ")");
        }

        Instant scheduledAt;
        try {
            scheduledAt = LocalDateTime.parse(params.scheduledAt()).atZone(KST).toInstant();
        } catch (DateTimeParseException | NullPointerException e) {
            return ToolResponse.fail("scheduledAt must be YYYY-MM-DDTHH:mm (KST), got: " + params.scheduledAt());
        }

        Schedule saved = scheduleWriter.create(ctx.userId(), ctx.turnId(), title, params.content(),
                ScheduleType.fromOrEtc(params.scheduleType()), scheduledAt);

        String line = "[%d] %s [%s] %s".formatted(
                saved.getScheduleId(), DISPLAY.format(saved.getScheduledAt()),
                saved.getScheduleType(), saved.getTitle());
        return ToolResponse.ok("등록됨: " + line, "schedule.add: " + line);
    }

    // --------------------------------------------------------------- update

    private static final int UPDATE_SEARCH_DAYS = 30;

    /**
     * 1단계 — 조회 + 확인 제안. DB는 변경하지 않는다.
     * 대상은 제목(+날짜)으로 특정한다 — 유저에게 내부 ID를 노출하지 않는다.
     */
    private ToolResponse update(ScheduleParams params, ToolContext ctx) {
        if (params.targetTitle() == null || params.targetTitle().isBlank()) {
            return ToolResponse.fail("targetTitle is required");
        }

        LocalDate searchFrom;
        LocalDate searchTo;   // inclusive
        try {
            if (params.targetDate() != null) {
                searchFrom = LocalDate.parse(params.targetDate());
                searchTo = searchFrom;
            } else {
                searchFrom = LocalDate.now(KST);
                searchTo = searchFrom.plusDays(UPDATE_SEARCH_DAYS);
            }
        } catch (DateTimeParseException e) {
            return ToolResponse.fail("targetDate must be YYYY-MM-DD, got: " + params.targetDate());
        }

        Instant newAt;
        try {
            newAt = params.scheduledAt() != null
                    ? LocalDateTime.parse(params.scheduledAt()).atZone(KST).toInstant() : null;
        } catch (DateTimeParseException e) {
            return ToolResponse.fail("scheduledAt must be YYYY-MM-DDTHH:mm (KST), got: " + params.scheduledAt());
        }
        if (newAt == null && params.title() == null && params.content() == null && params.scheduleType() == null) {
            return ToolResponse.fail("변경할 내용이 없습니다 — 무엇을 바꿀지 유저에게 확인받으세요");
        }

        List<Schedule> candidates = findByTitleInPeriod(ctx.userId(), params.targetTitle(), searchFrom, searchTo);
        if (candidates.isEmpty()) {
            return ToolResponse.fail("'%s' 일정을 찾지 못했습니다 (검색 범위: %s ~ %s)"
                    .formatted(params.targetTitle(), searchFrom, searchTo));
        }
        if (candidates.size() > 1) {
            return ToolResponse.fail("같은 제목의 일정이 여러 건입니다. 어떤 일정인지 유저에게 확인받으세요:\n"
                    + renderLines(candidates));
        }

        Schedule target = candidates.get(0);
        StringBuilder change = new StringBuilder();
        if (newAt != null) {
            change.append("\n- 시각: ").append(DISPLAY.format(target.getScheduledAt()))
                    .append(" → ").append(DISPLAY.format(newAt));
        }
        if (params.title() != null) {
            change.append("\n- 제목: ").append(target.getTitle()).append(" → ").append(params.title());
        }
        if (params.scheduleType() != null) {
            change.append("\n- 분류: ").append(target.getScheduleType())
                    .append(" → ").append(ScheduleType.fromOrEtc(params.scheduleType()));
        }
        if (params.content() != null) {
            change.append("\n- 상세: ").append(params.content());
        }

        String targetLine = renderLine(target);
        return ToolResponse.ok(
                "안내: 일정 변경은 한 번에 1건씩만 가능합니다. 유저가 여러 건 변경을 요청했다면 "
                        + "이 건부터 확인받고, 나머지는 이후 한 건씩 다시 확인받아 진행하세요.\n"
                        + "변경 확인 필요 — 아직 변경되지 않았습니다. 아래 내용을 날짜·시각·제목 표기 그대로 유저에게 "
                        + "보여주고 진행 여부를 확인받으세요.\n대상: " + targetLine + change,
                "schedule.update.confirm: " + targetLine + change.toString().replace("\n", " / "));
    }

    /** 2단계 — 유저 동의 후 적용. 새 row 추가 + 기존 row soft delete. */
    private ToolResponse applyUpdate(ScheduleParams params, ToolContext ctx) {
        if (params.targetTitle() == null || params.targetTitle().isBlank() || params.targetScheduledAt() == null) {
            return ToolResponse.fail("apply_update에는 targetTitle과 targetScheduledAt(변경 전 시각)이 필요합니다");
        }

        LocalDateTime targetLocal;
        try {
            targetLocal = LocalDateTime.parse(params.targetScheduledAt());
        } catch (DateTimeParseException e) {
            return ToolResponse.fail("targetScheduledAt must be YYYY-MM-DDTHH:mm (KST), got: "
                    + params.targetScheduledAt());
        }
        Instant targetAt = targetLocal.atZone(KST).toInstant();

        // 대상 날짜 하루 범위에서 제목 + 변경 전 시각 정확 매칭 — ID 없이 유일 특정
        List<Schedule> matches = findByTitleInPeriod(
                ctx.userId(), params.targetTitle(), targetLocal.toLocalDate(), targetLocal.toLocalDate())
                .stream().filter(s -> s.getScheduledAt().equals(targetAt)).toList();
        if (matches.isEmpty()) {
            return ToolResponse.fail("변경 대상(%s %s)을 찾지 못했습니다 — 변경 확인(update)부터 다시 진행하세요"
                    .formatted(params.targetScheduledAt(), params.targetTitle()));
        }
        if (matches.size() > 1) {
            return ToolResponse.fail("동일 제목·시각 일정이 여러 건이라 적용할 수 없습니다:\n" + renderLines(matches));
        }

        Schedule old = matches.get(0);
        Instant newAt;
        try {
            newAt = params.scheduledAt() != null
                    ? LocalDateTime.parse(params.scheduledAt()).atZone(KST).toInstant() : null;
        } catch (DateTimeParseException e) {
            return ToolResponse.fail("scheduledAt must be YYYY-MM-DDTHH:mm (KST), got: " + params.scheduledAt());
        }
        if (params.title() != null && params.title().length() > MAX_TITLE_LENGTH) {
            return ToolResponse.fail("title too long (max " + MAX_TITLE_LENGTH + ")");
        }
        ScheduleType newType = params.scheduleType() != null
                ? ScheduleType.fromOrEtc(params.scheduleType()) : null;

        // 교체 정책(새 row + 기존 soft delete)은 Writer 소유 — null 파라미터는 기존 값 유지
        Schedule saved = scheduleWriter.replace(old, ctx.turnId(), params.title(), params.content(), newType, newAt);
        ctx.progress("{\"stage\":\"APPLY\",\"oldScheduleId\":" + old.getScheduleId()
                + ",\"newScheduleId\":" + saved.getScheduleId() + "}");

        String line = renderLine(old) + " → " + renderLine(saved);
        return ToolResponse.ok("변경됨: " + line,
                "schedule.apply_update: old=" + old.getScheduleId() + " new=" + saved.getScheduleId() + " " + line);
    }

    /** 제목 매칭 기간 조회 — to는 inclusive. 매칭은 대소문자 무시 양방향 contains. */
    private List<Schedule> findByTitleInPeriod(Long userId, String title, LocalDate from, LocalDate toInclusive) {
        Instant f = from.atStartOfDay(KST).toInstant();
        Instant t = toInclusive.plusDays(1).atStartOfDay(KST).toInstant();
        String needle = title.strip().toLowerCase(Locale.ROOT);
        return scheduleRepository.findAllByUserIdInPeriod(userId, f, t).stream()
                .filter(s -> {
                    String stored = s.getTitle().toLowerCase(Locale.ROOT);
                    return stored.contains(needle) || needle.contains(stored);
                })
                .toList();
    }

    private String renderLine(Schedule s) {
        return DISPLAY.format(s.getScheduledAt()) + " [" + s.getScheduleType() + "] " + s.getTitle();
    }

    private String renderLines(List<Schedule> schedules) {
        return schedules.stream().map(s -> "- " + renderLine(s)).collect(Collectors.joining("\n"));
    }

    // ----------------------------------------------------------------- list

    private ToolResponse list(ScheduleParams params, ToolContext ctx) {
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = params.from() != null ? LocalDate.parse(params.from()) : LocalDate.now(KST);
            toDate = params.to() != null ? LocalDate.parse(params.to())
                    : fromDate.plusDays(LIST_DEFAULT_DAYS - 1);
        } catch (DateTimeParseException e) {
            return ToolResponse.fail("from/to must be YYYY-MM-DD, got: " + params.from() + " ~ " + params.to());
        }

        // to는 inclusive — 추출기/자연어("30일까지")와 의미를 맞춘다. from=to면 하루 조회.
        Instant from = fromDate.atStartOfDay(KST).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(KST).toInstant();
        List<Schedule> schedules = scheduleRepository.findAllByUserIdInPeriod(ctx.userId(), from, to);

        String period = fromDate + " ~ " + toDate;
        if (schedules.isEmpty()) {
            return ToolResponse.ok("해당 기간 일정 없음 (" + period + ")",
                    "schedule.list: 0건 (" + period + ")");
        }

        // 노출 캡 — 컨텍스트 크기 규약은 툴이 소유
        StringBuilder sb = new StringBuilder("일정 " + schedules.size() + "건 (" + period + ")");
        schedules.stream().limit(LIST_CAP).forEach(s -> sb.append("\n[%d] %s [%s] %s%s".formatted(
                s.getScheduleId(), DISPLAY.format(s.getScheduledAt()),
                s.getScheduleType(), s.getTitle(), s.isDone() ? " (완료)" : "")));
        if (schedules.size() > LIST_CAP) {
            sb.append("\n외 ").append(schedules.size() - LIST_CAP).append("건");
        }
        return ToolResponse.ok(sb.toString(), "schedule.list: " + schedules.size() + "건 (" + period + ")");
    }
}
