package com.yvan.cywan.anvilcord.consumer;

import com.yvan.cywan.anvilcord.core.AnvilCordPluginHost;
import org.springframework.boot.SpringApplication;

/**
 * Example host application that compiles against AnvilCord core contracts while
 * loading the full framework from {@code anvilcord-starter} at runtime.
 *
 * <p>Future plugin modules can be placed under this package, or the scan base
 * can be expanded by a real host application to include external plugin
 * packages.</p>
 */
@AnvilCordPluginHost
public class AnvilCordConsumerApplication {

    static void main(String[] args) {
        SpringApplication.run(AnvilCordConsumerApplication.class, args);
    }
}

