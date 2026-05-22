package com.yvan.cywan.anvilcord.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.annotation.AliasFor;

/**
 * Marks a Spring Boot application as an AnvilCord plugin host.
 *
 * <p>The host application only needs to compile against core API types. When
 * {@code anvilcord-starter} is present on the runtime classpath, Spring Boot
 * auto-configuration contributes the framework-owned Discord gateway, event bus,
 * command orchestration, and default commands.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootApplication
public @interface AnvilCordPluginHost {

    /**
     * Alias for {@link SpringBootApplication#scanBasePackages()}.
     */
    @AliasFor(annotation = SpringBootApplication.class, attribute = "scanBasePackages")
    String[] scanBasePackages() default {};

    /**
     * Alias for {@link SpringBootApplication#scanBasePackageClasses()}.
     */
    @AliasFor(annotation = SpringBootApplication.class, attribute = "scanBasePackageClasses")
    Class<?>[] scanBasePackageClasses() default {};
}
