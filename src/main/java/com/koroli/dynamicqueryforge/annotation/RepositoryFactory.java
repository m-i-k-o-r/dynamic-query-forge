package com.koroli.dynamicqueryforge.annotation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;

@Component
@RequiredArgsConstructor
public class RepositoryFactory {
    private final RepositoryProxy repositoryProxy;

    @SuppressWarnings("unchecked")
    public <T> T createRepository(Class<T> repoInterface) {
        if (!repoInterface.isInterface()) {
            throw new IllegalArgumentException("Требуется интерфейс для создания прокси");
        }

        return (T) Proxy.newProxyInstance(
                repoInterface.getClassLoader(),
                new Class<?>[]{repoInterface},
                repositoryProxy
        );
    }
}