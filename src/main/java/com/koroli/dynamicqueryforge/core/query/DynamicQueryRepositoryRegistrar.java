package com.koroli.dynamicqueryforge.core.query;

import com.koroli.dynamicqueryforge.core.repository.DynamicRepositoryFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

public class DynamicQueryRepositoryRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String[] basePackages = getBasePackages(importingClassMetadata);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.addIncludeFilter(new AnnotationTypeFilter(org.springframework.stereotype.Repository.class));

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String beanClassName = candidate.getBeanClassName();
                try {
                    Class<?> repositoryInterface = Class.forName(beanClassName);
                    if (repositoryInterface.isInterface()) {
                        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                                .genericBeanDefinition(DynamicRepositoryFactoryBean.class)
                                .addConstructorArgValue(repositoryInterface);

                        String beanName = StringUtils.uncapitalize(ClassUtils.getQualifiedName(repositoryInterface));
                        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Не удалось загрузить класс " + beanClassName, e);
                }
            }
        }
    }

    private String[] getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableDynamicQueryRepositories.class.getName());
        if (attributes != null) {
            Object value = attributes.get("basePackages");
            if (value instanceof String[] && ((String[]) value).length > 0) {
                return (String[]) value;
            }
        }

        return new String[]{ClassUtils.getPackageName(importingClassMetadata.getClassName())};
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}