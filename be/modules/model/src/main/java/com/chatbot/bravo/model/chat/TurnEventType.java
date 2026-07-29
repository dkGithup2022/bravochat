package com.chatbot.bravo.model.chat;

public enum TurnEventType {
    USER_MESSAGE,
    TOOL_CALL,
    TOOL_PROGRESS,
    TOOL_RESULT,
    ASSISTANT_MESSAGE
}
