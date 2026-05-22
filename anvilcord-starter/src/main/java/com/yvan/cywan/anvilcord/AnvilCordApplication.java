package com.yvan.cywan.anvilcord;


import com.yvan.cywan.anvilcord.core.AnvilCordPluginHost;
import org.springframework.boot.SpringApplication;

@AnvilCordPluginHost
public class AnvilCordApplication {

    static void main(String[] args) {
        SpringApplication.run(AnvilCordApplication.class, args);
    }
}