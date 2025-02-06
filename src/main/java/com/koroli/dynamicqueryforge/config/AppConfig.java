package com.koroli.dynamicqueryforge.config;

import com.koroli.dynamicqueryforge.executor.DatabaseType;
import com.koroli.queryconverter.model.FieldType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.koroli.queryconverter.converters.QueryConverter;

@Configuration
public class AppConfig {

    /** Тип базы данных */
    @Value("${database.type}")
    private String databaseType;

    /** Тип поля по умолчанию */
    @Value("${query.converter.defaultFieldType:STRING}")
    private String defaultFieldType;

    /** Использование диска при агрегации */
    @Value("${query.converter.aggregationAllowDiskUse:true}")
    private boolean aggregationAllowDiskUse;

    /** Размер пакета для агрегации */
    @Value("${query.converter.aggregationBatchSize:1000}")
    private int aggregationBatchSize;

    /** Логирование конвертации запросов для MongoDB */
    @Value("${query.converter.logQueryEnabled:false}")
    private boolean logQueryEnabled;

    /** Логирование запросов */
    @Value("${database.logQueryEnabled:false}")
    private boolean logModifiedQueryEnabled;

    @Bean
    public QueryConverter queryConverter() {
        return QueryConverter.builder()
                .defaultFieldType(FieldType.valueOf(defaultFieldType.toUpperCase()))
                .aggregationAllowDiskUse(aggregationAllowDiskUse)
                .aggregationBatchSize(aggregationBatchSize)
                .logQueryEnabled(logQueryEnabled)
                .build();
    }

    @Bean
    public DatabaseType databaseType() {
        return DatabaseType.valueOf(databaseType.toUpperCase());
    }

    @Bean
    public boolean logModifiedQueryEnabled() {
        return logModifiedQueryEnabled;
    }
}