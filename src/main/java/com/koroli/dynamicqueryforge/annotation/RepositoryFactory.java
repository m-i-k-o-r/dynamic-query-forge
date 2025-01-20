package com.koroli.dynamicqueryforge.annotation;

import java.lang.reflect.Proxy;

public class RepositoryFactory {

    @SuppressWarnings("unchecked")
    public static <T> T createRepository(Class<T> repoInterface) {
        if (!repoInterface.isInterface()) {
            throw new IllegalArgumentException("Требуется интерфейс для создания прокси");
        }

        return (T) Proxy.newProxyInstance(
                repoInterface.getClassLoader(),
                new Class<?>[]{repoInterface},
                new RepositoryProxy()
        );
    }
}