package com.dmc.archiving.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine cache
 * Provides in-memory caching for frequently accessed data
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "archives",
                "archiveWithRelations",
                "users",
                "elements",
                "userAssignments"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)  // Maximum 1000 entries per cache
                .expireAfterWrite(10, TimeUnit.MINUTES)  // Expire after 10 minutes
                .recordStats());  // Enable cache statistics

        return cacheManager;
    }

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }
}
