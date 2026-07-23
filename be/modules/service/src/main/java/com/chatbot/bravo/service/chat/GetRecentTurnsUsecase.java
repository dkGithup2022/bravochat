package com.chatbot.bravo.service.chat;

import com.chatbot.bravo.exception.chat.InvalidRecentTurnSizeException;
import com.chatbot.bravo.infrastructure.chat.repository.RecentTurnQueryRepository;
import com.chatbot.bravo.model.chat.RecentTurn;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsQuery;
import com.chatbot.bravo.service.chat.dto.GetRecentTurnsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetRecentTurnsUsecase {

    static final int MAX_SIZE = 20;

    private final RecentTurnQueryRepository recentTurnQueryRepository;

    /**
     * 인증된 유저의 최근 완료 대화를 최대 size개 조회한다. (오래된→최신)
     * size 범위(1~20) 위반 시 InvalidRecentTurnSizeException(400).
     */
    public GetRecentTurnsResult getRecentTurns(GetRecentTurnsQuery query) {
        int size = query.size();
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRecentTurnSizeException("size out of range(1~" + MAX_SIZE + "): " + size);
        }

        List<RecentTurn> turns = recentTurnQueryRepository.findRecentCompletedTurns(query.userId(), size);
        return new GetRecentTurnsResult(turns);
    }
}
