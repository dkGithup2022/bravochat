rootProject.name = "chatbot-bravo-be"

include(
    ":modules:applications:api-application",
    ":modules:api",
    ":modules:service",
    ":modules:repository-jdbc",
    ":modules:infrastructure",
    ":modules:model",
    ":modules:schema",
    ":modules:exception",
    ":modules:llm-openai",
)
