package com.koroli.dynamicqueryforge.annotation;

import com.koroli.dynamicqueryforge.repository.DynamicQueryRepositoryRegistrar;
import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

/**
 * Аннотация для активации динамических репозиториев.
 * Указывает, какие пакеты следует сканировать на наличие интерфейсов репозиториев.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(DynamicQueryRepositoryRegistrar.class)
public @interface EnableDynamicQueryRepositories {

    /**
     * Пакеты для сканирования интерфейсов репозиториев.
     * Если не указано, будет сканироваться пакет, содержащий класс, аннотированный этой аннотацией.
     *
     * @return массив строк с именами пакетов
     */
    String[] basePackages() default {};
}