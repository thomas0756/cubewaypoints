package com.amplicube.cubewaypoints.client.config;

import com.amplicube.cubewaypoints.Cubewaypoints;

import com.amplicube.cubewaypoints.client.render.BoxRenderer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;


@Config(name = Cubewaypoints.MOD_ID)
public class CubeWaypointsConfig implements ConfigData {

    @ConfigEntry.ColorPicker(allowAlpha = true)
    public int primaryColour = 0x4DFF0000;

    @ConfigEntry.ColorPicker(allowAlpha = true)
    public int secondaryColour = 0x4D00FF00;

    public float outlineWidth = 1f;
    public float outlineAlpha = 1f;

    public static void onInitialize() {
        AutoConfig.register(CubeWaypointsConfig.class, Toml4jConfigSerializer::new);
        AutoConfig.getConfigHolder(CubeWaypointsConfig.class).registerSaveListener((configHolder, config) -> {
            onConfigSaved(config);
            return null;
        });
    }

    public static void onConfigSaved(CubeWaypointsConfig config) {
        BoxRenderer.updateSettings();
    }
}