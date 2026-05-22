package com.yvan.cywan.anvilcord;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class AnvilCordApplication {

    static void main(String[] args) {
        SpringApplication.run(AnvilCordApplication.class, args);
    }
}