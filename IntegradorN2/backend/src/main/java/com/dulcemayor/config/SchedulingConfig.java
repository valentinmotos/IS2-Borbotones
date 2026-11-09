package com.dulcemayor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Habilita @Scheduled (newsletter mensual del día 5, E5-05).
 */
@Configuration
@EnableScheduling
@EnableCaching
public class SchedulingConfig {
}
