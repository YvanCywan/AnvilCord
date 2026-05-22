package com.yvan.cywan.anvilcord.starter;

import com.yvan.cywan.anvilcord.core.event.VirtualEventBus;
import com.yvan.cywan.anvilcord.discord.DiscordGatewayBridge;
import com.yvan.cywan.anvilcord.discord.config.BotCoreProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Wires AnvilCord's framework-owned beans when {@code anvilcord-starter} is on
 * a Spring Boot application's runtime classpath.
 */
@AutoConfiguration
@EnableConfigurationProperties(BotCoreProperties.class)
@ComponentScan(basePackageClasses = {
		VirtualEventBus.class,
		DiscordGatewayBridge.class,
		FrameworkInitializationPublisher.class
})
public class AnvilCordFrameworkAutoConfiguration {

	@Bean
	static AnvilCordSlashCommandBeanRegistrar anvilCordSlashCommandBeanRegistrar() {
		return new AnvilCordSlashCommandBeanRegistrar();
	}

	@Bean
	static AnvilCordPluginBeanRegistrar anvilCordPluginBeanRegistrar() {
		return new AnvilCordPluginBeanRegistrar();
	}

	@Bean
	AnvilCordPluginInitializer anvilCordPluginInitializer(VirtualEventBus eventBus) {
		return new AnvilCordPluginInitializer(eventBus);
	}
}

