package com.dulcemayor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita @Scheduled (newsletter mensual del día 5, E5-05).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
