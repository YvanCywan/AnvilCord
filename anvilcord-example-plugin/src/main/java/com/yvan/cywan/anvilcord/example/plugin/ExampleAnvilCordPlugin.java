package com.yvan.cywan.anvilcord.example.plugin;

import com.yvan.cywan.anvilcord.core.plugin.AnvilCordPlugin;

/**
 * Sample plugin descriptor discovered through Java's ServiceLoader.
 */
public final class ExampleAnvilCordPlugin implements AnvilCordPlugin {

    @Override
    public String id() {
        return "example-plugin";
    }
}

