package com.koroli.dynamicqueryforge.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.koroli.dynamicqueryforge.config.DynamicQueryProperties;
import net.sf.jsqlparser.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
public class QueryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryCache.class);

    private final Cache<String, Statement> cache;

    public QueryCache(DynamicQueryProperties properties) {
        cache = Caffeine.newBuilder()
                .maximumSize(properties.getCache().getMaxSize())
                .scheduler(Scheduler.systemScheduler())
                .evictionListener((key, value, cause) -> {
                    if (properties.getCache().isLogEvictions()) {
                        LOGGER.debug("Cache: [{}] was deleted due to {}", key, cause);
                    }
                })
                .recordStats()
                .build();
    }

    public Statement get(String key) {
        return cache.getIfPresent(key);
    }

    public void put(String key, Statement statement) {
        cache.put(key, statement);
    }

    public void invalidate(String key) {
        cache.invalidate(key);
    }
}
