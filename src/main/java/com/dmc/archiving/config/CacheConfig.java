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
                "archives",             // Individual archives
                "archiveWithRelations", // Archives with relationships
                "archivesByTenant",     // Archives filtered by tenant
                "archivesByOwner",      // Archives filtered by owner
                "users",                // User details
                "tenants",              // Tenant details
                "elements",             // Archive elements
                "userAssignments",      // User assignments
                "archiveStatistics",    // Dashboard statistics
                "documents"             // Documents
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)  // Maximum 1000 entries per cache
                .expireAfterWrite(10, TimeUnit.MINUTES)  // Expire after 10 minutes
                .recordStats());  // Enable cache statistics for monitoring

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
