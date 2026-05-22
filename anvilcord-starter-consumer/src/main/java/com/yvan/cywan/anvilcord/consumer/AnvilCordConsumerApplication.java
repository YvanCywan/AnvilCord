package com.yvan.cywan.anvilcord.consumer;

import com.yvan.cywan.anvilcord.AnvilCordApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example host application that consumes {@code anvilcord-starter} without
 * publishing an artifact of its own.
 *
 * <p>Future plugin modules can be placed under this package, or the scan base
 * can be expanded by a real host application to include external plugin
 * packages.</p>
 */
@SpringBootApplication(scanBasePackageClasses = AnvilCordApplication.class)
public class AnvilCordConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnvilCordConsumerApplication.class, args);
    }
}

