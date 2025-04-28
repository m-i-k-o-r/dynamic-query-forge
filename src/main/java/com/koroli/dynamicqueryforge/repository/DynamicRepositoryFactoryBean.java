package com.koroli.dynamicqueryforge.repository;

import com.koroli.dynamicqueryforge.processing.RepositoryMethodInterceptor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Proxy;

/**
 * Фабрика для создания прокси-объектов репозиториев.
 */
public class DynamicRepositoryFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> repositoryInterface;

    @Autowired
    private RepositoryMethodInterceptor dynamicRepositoryProxy;

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
