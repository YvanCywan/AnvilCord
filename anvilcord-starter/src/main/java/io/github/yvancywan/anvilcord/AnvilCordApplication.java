package io.github.yvancywan.anvilcord;


import io.github.yvancywan.anvilcord.core.AnvilCordPluginHost;
import org.springframework.boot.SpringApplication;

@AnvilCordPluginHost
public class AnvilCordApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnvilCordApplication.class, args);
    }
}