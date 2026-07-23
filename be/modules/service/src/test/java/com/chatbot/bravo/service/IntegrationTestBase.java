package com.chatbot.bravo.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = IntegrationConfig.class)
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {}
