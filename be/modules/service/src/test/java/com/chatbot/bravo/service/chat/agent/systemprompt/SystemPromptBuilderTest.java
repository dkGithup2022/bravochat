package com.chatbot.bravo.service.chat.agent.systemprompt;

import com.chatbot.bravo.service.chat.agent.systemprompt.sectionContributor.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptBuilderTest {

    private SystemPromptBuilder builderFor(RuntimeContext ctx) {
        return new DefaultSystemPromptBuilder(
                new DefaultCoreSectionContributor(ctx),
                new DefaultPersonaSectionContributor(ctx),
                new DefaultToolGuidanceContributor(),
                new DefaultStyleSectionContributor(),
                new DefaultResponseFormatContributor());
    }

    private SortedSet<String> tools(String... names) {
        return new TreeSet<>(List.of(names));
    }

    @Test
    void 도구있음_섹션_순서와_캐시경계() {
        var ctx = new RuntimeContext("주문봇", "ShopKr", Optional.of("한국어"));
        List<PromptSection> s = builderFor(ctx).build(ctx, tools("lookup_orders", "cancel_order"));

        assertThat(s).extracting(PromptSection::name)
                .containsExactly("core", "persona", "tool_guidance", "style", "response_format");
        assertThat(s).extracting(PromptSection::cacheable)
                .containsExactly(true, true, false, false, false); // cacheable 은 앞쪽에 연속
    }

    @Test
    void 결정성_같은입력이면_바이트동일() {
        var ctx = new RuntimeContext("주문봇", "ShopKr", Optional.of("한국어"));
        var t = tools("lookup_orders", "cancel_order");
        assertThat(builderFor(ctx).build(ctx, t)).isEqualTo(builderFor(ctx).build(ctx, t));
    }

    @Test
    void 부재_도구없으면_tool_guidance_생략() {
        var ctx = new RuntimeContext("주문봇", "ShopKr", Optional.of("한국어"));
        List<PromptSection> s = builderFor(ctx).build(ctx, tools());
        assertThat(s).extracting(PromptSection::name)
                .containsExactly("core", "persona", "style", "response_format"); // tool_guidance 부재
    }

    @Test
    void 부재_언어없으면_persona에_언어섹션_없음() {
        var ctx = new RuntimeContext("주문봇", "ShopKr", Optional.empty());
        List<PromptSection> s = builderFor(ctx).build(ctx, tools());
        String persona = s.stream().filter(x -> x.name().equals("persona")).findFirst().orElseThrow().text();
        assertThat(persona).doesNotContain("# 언어");
    }

    @Test
    void 재현_주문봇_샘플_조립() {
        var ctx = new RuntimeContext("주문봇", "ShopKr", Optional.of("한국어"));
        String prompt = builderFor(ctx).build(ctx, tools("lookup_orders", "cancel_order"))
                .stream().map(PromptSection::text).reduce((a, b) -> a + "\n\n" + b).orElseThrow();

        assertThat(prompt)
                .startsWith("당신은 ShopKr의 AI 어시스턴트 '주문봇'입니다.")
                .contains("# 시스템 정보와 외부 데이터 처리")
                .contains("# 당신은 누구인가")
                .contains("항상 한국어로 응답하세요")
                .contains("# 도구 사용")
                .contains("# 톤")
                .contains("# 출력");
    }
}
