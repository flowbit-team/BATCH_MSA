package com.example.batchservice.service;

import com.example.batchservice.constants.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TemplateCache {
    private static final Logger logger = LoggerFactory.getLogger(TemplateCache.class);
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);

    public void setBaseTemplate(String template) {
        cache.put(AppConstants.Template.BASE_TEMPLATE_KEY, new CacheEntry(template));
        logger.debug("Base template has been updated in cache");
    }

    public String getBaseTemplate() {
        CacheEntry entry = cache.get(AppConstants.Template.BASE_TEMPLATE_KEY);
        if (entry == null) {
            cacheMisses.incrementAndGet();
            logger.warn("Base template not found in cache");
            return null;
        }
        cacheHits.incrementAndGet();
        entry.updateLastAccessed();
        return entry.getTemplate();
    }

    public boolean hasBaseTemplate() {
        boolean hasTemplate = cache.containsKey(AppConstants.Template.BASE_TEMPLATE_KEY);
        logger.trace("Checking if base template exists in cache: {}", hasTemplate);
        return hasTemplate;
    }

    @Scheduled(cron = "0 0 6 * * *") // 매일 오전 6시에 실행
    public void resetCache() {
        logger.info("Resetting template cache. Stats before reset - Hits: {}, Misses: {}", 
            cacheHits.get(), cacheMisses.get());
        cache.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
        logger.info("Template cache has been reset successfully");
    }

    private static class CacheEntry {
        private final String template;
        private LocalDateTime lastAccessed;

        public CacheEntry(String template) {
            this.template = template;
            this.lastAccessed = LocalDateTime.now();
        }

        public String getTemplate() {
            return template;
        }

        public void updateLastAccessed() {
            this.lastAccessed = LocalDateTime.now();
        }

        public LocalDateTime getLastAccessed() {
            return lastAccessed;
        }
    }
} 