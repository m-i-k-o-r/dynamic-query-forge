package com.koroli.dynamicqueryforge.core.query;

import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DynamicQueryRepositoryRegistrar.class)
public @interface EnableDynamicQueryRepositories {
    /**
     * Пакеты для сканирования интерфейсов репозиториев.
     * Если не указано, будет сканироваться пакет, где находится класс,
     * аннотированный @EnableDynamicQueryRepositories.
     */
    String[] basePackages() default {};
}