package com.koroli.dynamicqueryforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "dynamic-query")
public class DynamicQueryProperties {

    /** Настройки кеширования запросов */
    @NestedConfigurationProperty
    private CacheProperties cache = new CacheProperties();

    /** Включение логирования выполняемых запросов */
    private boolean logQueries = false;

    /**
     * Настройки кеширования
     */
    @Getter
    @Setter
    public static class CacheProperties {
        /** Максимальное количество элементов */
        private int maxSize = 100;

        /** Флаг включения/отключения логирования удаления элемента из кэша */
        private boolean logEvictions = false;
    }
}