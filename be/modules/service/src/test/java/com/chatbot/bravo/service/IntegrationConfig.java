package com.chatbot.bravo.service;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication
@EnableJdbcAuditing
@ComponentScan(basePackages = {
    "com.chatbot.bravo.service",
    "com.chatbot.bravo.jdbc"
})
@EnableJdbcRepositories(basePackages = "com.chatbot.bravo.jdbc")
class IntegrationConfig {}
