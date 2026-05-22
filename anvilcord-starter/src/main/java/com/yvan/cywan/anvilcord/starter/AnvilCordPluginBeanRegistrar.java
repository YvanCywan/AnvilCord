package com.yvan.cywan.anvilcord.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * Registers Spring component beans contributed by runtime AnvilCord plugins.
 */
@SuppressWarnings("NullableProblems")
public final class AnvilCordPluginBeanRegistrar implements BeanDefinitionRegistryPostProcessor,
        EnvironmentAware, ResourceLoaderAware, Ordered {

    private static final String GENERATED_BEAN_NAME_ATTRIBUTE =
            ConfigurationClassPostProcessor.class.getName() + ".configurationClassPostProcessor.beanName";

    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, true);
        if (environment != null) {
            scanner.setEnvironment(environment);
        }
        scanner.setResourceLoader(resourceLoader);
        scanner.setBeanNameGenerator((definition, ignored) -> pluginBeanName(definition, registry));

        for (String basePackage : AnvilCordPluginCatalog.scanBasePackages(resourceLoader)) {
            scanner.scan(basePackage);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    private static String pluginBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        String beanClassName = definition.getBeanClassName();
        if (beanClassName == null) {
            return BeanDefinitionReaderUtils.generateBeanName(definition, registry);
        }

        Object generatedBeanName = definition.getAttribute(GENERATED_BEAN_NAME_ATTRIBUTE);
        String beanName;
        if (generatedBeanName instanceof String generated && !generated.isBlank()) {
            beanName = generated;
        } else {
            String simpleName = beanClassName.substring(beanClassName.lastIndexOf('.') + 1);
            beanName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        }
        return BeanDefinitionReaderUtils.uniqueBeanName(beanName, registry);
    }
}


