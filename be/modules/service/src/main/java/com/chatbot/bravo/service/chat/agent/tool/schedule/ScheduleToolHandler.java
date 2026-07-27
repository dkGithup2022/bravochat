package com.chatbot.bravo.service.chat.agent.tool.schedule;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import com.chatbot.bravo.service.chat.agent.tool.AbstractToolHandler;
import com.chatbot.bravo.service.chat.agent.tool.ToolContext;
import com.chatbot.bravo.service.chat.agent.tool.ToolResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * schedule 툴 — 일정 등록(add)/조회(list). 블랙박스: 외부는 name/description만 안다.
 * 오퍼레이션 선택·인자 해석은 추출 콜(paramSpec)이, 검증·시간 변환·조회 정책은 여기가 소유.
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
              * scheduledAt은 한국시간(Asia/Seoul) 기준. 시각 언급이 없으면 op=missing으로.
            - 일정 조회 요청이면: {"op":"list","from":"YYYY-MM-DD","to":"YYYY-MM-DD"}
              * 기간 언급이 없으면 from/to 생략 (기본: 오늘부터 7일).
            - 등록에 필요한 정보(제목 또는 시각)가 대화에 없으면:
              {"op":"missing","question":"<유저에게 물을 한 문장>"}""";

    private final ScheduleRepository scheduleRepository;

    public ScheduleToolHandler(ToolParamExtractor paramExtractor, ScheduleRepository scheduleRepository) {
        super(paramExtractor);
        this.scheduleRepository = scheduleRepository;
    }

    @Override public String name() { return "schedule"; }

    @Override public String promptText() {
        return "유저의 일정을 관리한다 — 일정 등록(예: \"내일 3시 회의 잡아줘\")과 조회(예: \"이번 주 일정 뭐 있지?\")";
    }

    @Override protected String paramSpec() { return PARAM_SPEC; }

    @Override protected Class<ScheduleParams> paramType() { return ScheduleParams.class; }

    @Override
    protected ToolResponse doToolLogic(ScheduleParams params, ToolContext ctx) {
        return switch (params.op() == null ? "" : params.op()) {
            case "add" -> add(params, ctx);
            case "list" -> list(params, ctx);
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

        Schedule saved = scheduleRepository.save(Schedule.create(
                ctx.userId(), ctx.turnId(), title, params.content(),
                ScheduleType.fromOrEtc(params.scheduleType()), scheduledAt));

        String line = "[%d] %s [%s] %s".formatted(
                saved.getScheduleId(), DISPLAY.format(saved.getScheduledAt()),
                saved.getScheduleType(), saved.getTitle());
        return ToolResponse.ok("등록됨: " + line, "schedule.add: " + line);
    }

    // ----------------------------------------------------------------- list

    private ToolResponse list(ScheduleParams params, ToolContext ctx) {
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = params.from() != null ? LocalDate.parse(params.from()) : LocalDate.now(KST);
            toDate = params.to() != null ? LocalDate.parse(params.to()) : fromDate.plusDays(LIST_DEFAULT_DAYS);
        } catch (DateTimeParseException e) {
            return ToolResponse.fail("from/to must be YYYY-MM-DD, got: " + params.from() + " ~ " + params.to());
        }

        Instant from = fromDate.atStartOfDay(KST).toInstant();
        Instant to = toDate.atStartOfDay(KST).toInstant();
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
