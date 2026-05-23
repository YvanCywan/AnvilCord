package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;
import io.github.yvancywan.anvilcord.discord.command.SlashCommandDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers lightweight {@link SlashCommandDefinition} beans for classes marked with {@link SlashCommand}.
 */
@SuppressWarnings("NullableProblems")
public final class SlashCommandAnnotationBeanRegistrar implements BeanDefinitionRegistryPostProcessor,
        BeanFactoryAware, EnvironmentAware, ResourceLoaderAware, Ordered {

    private ConfigurableListableBeanFactory beanFactory;
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableListableBeanFactory configurableBeanFactory) {
            this.beanFactory = configurableBeanFactory;
        }
    }

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
        return Ordered.LOWEST_PRECEDENCE - 2;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<String> commandClassNames = findSlashCommandClassNames();
        for (String commandClassName : commandClassNames) {
            registerSlashCommandDefinition(commandClassName, registry);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    private Set<String> findSlashCommandClassNames() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SlashCommand.class));
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }

        Set<String> commandClassNames = new LinkedHashSet<>();
        for (String basePackage : scanBasePackages()) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                String beanClassName = candidate.getBeanClassName();
                if (beanClassName != null && !beanClassName.isBlank()) {
                    commandClassNames.add(beanClassName);
                }
            }
        }
        return commandClassNames;
    }

    private Set<String> scanBasePackages() {
        Set<String> basePackages = new LinkedHashSet<>();
        basePackages.add(PingCommand.class.getPackageName());
        if (beanFactory != null && AutoConfigurationPackages.has(beanFactory)) {
            basePackages.addAll(AutoConfigurationPackages.get(beanFactory));
        }
        basePackages.addAll(AnvilCordPluginCatalog.scanBasePackages(resourceLoader));
        return basePackages;
    }

    private void registerSlashCommandDefinition(String commandClassName, BeanDefinitionRegistry registry) {
        Class<?> commandClass = commandClass(commandClassName);
        SlashCommand slashCommand = commandClass.getAnnotation(SlashCommand.class);
        if (slashCommand == null) {
            return;
        }

        RootBeanDefinition definition = new RootBeanDefinition(SlashCommandDefinitionFactory.class);
        definition.setFactoryMethodName("fromAnnotation");
        definition.getConstructorArgumentValues().addIndexedArgumentValue(0, commandClassName, String.class.getName());
        definition.setSource(commandClassName);

        String beanName = BeanDefinitionReaderUtils.uniqueBeanName(commandClassName + "#slashCommandDefinition", registry);
        registry.registerBeanDefinition(beanName, definition);
    }

    private static Class<?> commandClass(String commandClassName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = SlashCommandAnnotationBeanRegistrar.class.getClassLoader();
            }
            return Class.forName(commandClassName, false, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to load @SlashCommand class " + commandClassName, exception);
        }
    }

}






