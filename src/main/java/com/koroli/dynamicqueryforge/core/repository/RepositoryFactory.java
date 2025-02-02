package com.koroli.dynamicqueryforge.core.repository;

import com.koroli.dynamicqueryforge.core.query.DynamicRepositoryProxy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;

@Component
@RequiredArgsConstructor
public class RepositoryFactory {
    private final DynamicRepositoryProxy dynamicRepositoryProxy;

    @SuppressWarnings("unchecked")
    public <T> T createRepository(Class<T> repoInterface) {
        if (!repoInterface.isInterface()) {
            throw new IllegalArgumentException("Требуется интерфейс для создания прокси");
        }

        return (T) Proxy.newProxyInstance(
                repoInterface.getClassLoader(),
                new Class<?>[]{repoInterface},
                dynamicRepositoryProxy
        );
    }
}