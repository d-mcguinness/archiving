package com.dmc.archiving.usage;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduler so {@link UsageSnapshotJob} runs. Scoped to the
 * usage module rather than the application class to keep the concern local.
 */
@Configuration
@EnableScheduling
public class UsageConfig {
}
