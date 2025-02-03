package com.koroli.dynamicqueryforge.core.query;

import com.koroli.dynamicqueryforge.core.repository.DynamicQueryRepository;
import com.koroli.dynamicqueryforge.core.repository.DynamicRepositoryFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Map;

public class DynamicQueryRepositoryRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String[] basePackages = getBasePackages(importingClassMetadata);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String basePackage : basePackages) {
            try {
                String packageSearchPath = "classpath*:" + ClassUtils.convertClassNameToResourcePath(basePackage) + "/**/*.class";
                Resource[] resources = resolver.getResources(packageSearchPath);

                if (resources.length == 0) {
                    System.out.println("No resources found in package: " + basePackage);
                    continue;
                }

                System.out.println("Found resources: " + Arrays.toString(resources));
                for (Resource resource : resources) {
                    String className = extractClassName(resource, basePackage);
                    System.out.println("Candidate class name: " + className);

                    Class<?> repositoryInterface = Class.forName(className);
                    if (repositoryInterface.isInterface() && DynamicQueryRepository.class.isAssignableFrom(repositoryInterface)) {
                        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                                .genericBeanDefinition(DynamicRepositoryFactoryBean.class)
                                .addConstructorArgValue(repositoryInterface);

                        String beanName = ClassUtils.getShortNameAsProperty(repositoryInterface);
                        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при сканировании пакета: " + basePackage, e);
            }
        }
    }

    private String extractClassName(Resource resource, String basePackage) throws Exception {
        String resourcePath = resource.getURI().toString();
        String classPath = resourcePath.substring(resourcePath.indexOf("/classes/") + 9)
                .replace("/", ".")
                .replace(".class", "");
        return classPath;
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