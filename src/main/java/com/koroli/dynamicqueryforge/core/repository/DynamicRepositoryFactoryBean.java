package com.koroli.dynamicqueryforge.core.repository;

import com.koroli.dynamicqueryforge.core.query.DynamicRepositoryProxy;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Proxy;

/**
 * Фабрика для создания прокси-объектов репозиториев.
 */
public class DynamicRepositoryFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> repositoryInterface;

    @Autowired
    private DynamicRepositoryProxy dynamicRepositoryProxy;

    public DynamicRepositoryFactoryBean(Class<T> repositoryInterface) {
        this.repositoryInterface = repositoryInterface;
    }

    @Override
    public T getObject() {
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                dynamicRepositoryProxy);
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
