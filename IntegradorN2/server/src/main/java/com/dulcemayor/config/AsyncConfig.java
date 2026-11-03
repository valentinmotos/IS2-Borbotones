package com.dulcemayor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita @Async (EmailService.enviar, E1-06). Usa el executor que configura Spring Boot
 * (spring.task.execution.*), así un envío lento no frena la operación que lo pidió.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
