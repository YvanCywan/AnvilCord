package io.github.yvancywan.anvilcord.starter;

import io.github.yvancywan.anvilcord.discord.command.SlashCommand;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers concrete {@link SlashCommand} implementations without requiring
 * plugin authors to annotate every command as a Spring component.
 */
@SuppressWarnings("NullableProblems")
public final class AnvilCordSlashCommandBeanRegistrar implements BeanDefinitionRegistryPostProcessor,
        BeanFactoryAware, EnvironmentAware, ResourceLoaderAware, Ordered {

    private static final String GENERATED_BEAN_NAME_ATTRIBUTE =
            ConfigurationClassPostProcessor.class.getName() + ".configurationClassPostProcessor.beanName";

    private BeanFactory beanFactory;
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
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
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        if (environment != null) {
            scanner.setEnvironment(environment);
        }
        scanner.addIncludeFilter(new AssignableTypeFilter(SlashCommand.class));
        scanner.setResourceLoader(resourceLoader);

        for (String basePackage : commandBasePackages()) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                registerIfAbsent(registry, candidate);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    private Set<String> commandBasePackages() {
        Set<String> basePackages = new LinkedHashSet<>();
        if (beanFactory != null) {
            try {
                basePackages.addAll(AutoConfigurationPackages.get(beanFactory));
            } catch (IllegalStateException ignored) {
                // A non-Spring-Boot context can still use the built-in commands.
            }
        }
        basePackages.addAll(AnvilCordPluginCatalog.scanBasePackages(resourceLoader));
        basePackages.add(PingCommand.class.getPackageName());
        return basePackages;
    }

    private static void registerIfAbsent(BeanDefinitionRegistry registry, BeanDefinition candidate) {
        String beanClassName = candidate.getBeanClassName();
        if (beanClassName == null || beanClassName.equals(SlashCommand.class.getName())) {
            return;
        }
        if (containsBeanClass(registry, beanClassName)) {
            return;
        }

        String beanName = commandBeanName(candidate, beanClassName);
        beanName = BeanDefinitionReaderUtils.uniqueBeanName(beanName, registry);
        registry.registerBeanDefinition(beanName, candidate);
    }

    private static boolean containsBeanClass(BeanDefinitionRegistry registry, String beanClassName) {
        for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            if (beanClassName.equals(beanDefinition.getBeanClassName())) {
                return true;
            }
        }
        return false;
    }

    private static String commandBeanName(BeanDefinition candidate, String beanClassName) {
        Object generatedBeanName = candidate.getAttribute(GENERATED_BEAN_NAME_ATTRIBUTE);
        if (generatedBeanName instanceof String beanName && !beanName.isBlank()) {
            return beanName;
        }

        String simpleName = beanClassName.substring(beanClassName.lastIndexOf('.') + 1);
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}


