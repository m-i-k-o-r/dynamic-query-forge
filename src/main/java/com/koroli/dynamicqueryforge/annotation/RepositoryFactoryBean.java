package com.koroli.dynamicqueryforge.annotation;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.Proxy;

public class RepositoryFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> repositoryInterface;

    @Autowired
    private RepositoryProxy repositoryProxy; // Этот бин должен быть помечен как @Component

    public RepositoryFactoryBean(Class<T> repositoryInterface) {
        this.repositoryInterface = repositoryInterface;
    }

    @Override
    public T getObject() {
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class<?>[]{repositoryInterface},
                repositoryProxy);
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