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

import java.util.Map;

/**
 * Регистратор динамических репозиториев для работы с запросами.
 */
public class DynamicQueryRepositoryRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    /**
     * Регистрирует определения бинов для интерфейсов репозиториев, которые наследуются от {@link DynamicQueryRepository}.
     *
     * @param importingClassMetadata метаданные класса-потомка
     * @param registry               реестр определения бинов
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String[] basePackages = getBasePackages(importingClassMetadata);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String basePackage : basePackages) {
            try {
                String packageSearchPath = "classpath*:" + ClassUtils.convertClassNameToResourcePath(basePackage) + "/**/*.class";
                Resource[] resources = resolver.getResources(packageSearchPath);

                for (Resource resource : resources) {
                    String className = extractClassName(resource, basePackage);
                    Class<?> repositoryInterface = Class.forName(className);

                    if (repositoryInterface.isInterface() && DynamicQueryRepository.class.isAssignableFrom(repositoryInterface)) {
                        registerRepositoryBeanDefinition(registry, repositoryInterface);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при сканировании пакета: " + basePackage, e);
            }
        }
    }

    /**
     * Извлекает имя класса из ресурса и имени пакета.
     *
     * @param resource    ресурс с классом
     * @param basePackage базовый пакет для поиска
     * @return полное имя класса
     * @throws Exception если не удалось извлечь имя класса
     */
    private String extractClassName(Resource resource, String basePackage) throws Exception {
        String resourcePath = resource.getURI().toString();
        return resourcePath.substring(resourcePath.indexOf("/classes/") + 9)
                .replace("/", ".")
                .replace(".class", "");
    }

    /**
     * Извлекает базовые пакеты для сканирования из метаданных аннотации.
     *
     * @param importingClassMetadata метаданные класса
     * @return массив строк с именами базовых пакетов
     */
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

    /**
     * Регистрирует бин для интерфейса репозитория.
     *
     * @param registry            реестр определения бинов
     * @param repositoryInterface интерфейс репозитория
     */
    private void registerRepositoryBeanDefinition(BeanDefinitionRegistry registry, Class<?> repositoryInterface) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(DynamicRepositoryFactoryBean.class)
                .addConstructorArgValue(repositoryInterface);

        String beanName = ClassUtils.getShortNameAsProperty(repositoryInterface);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
