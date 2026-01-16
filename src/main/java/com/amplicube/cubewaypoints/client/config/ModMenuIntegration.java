package com.amplicube.cubewaypoints.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.autoconfig.AutoConfig;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;


@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @SuppressWarnings("removal")
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {

        return parent -> AutoConfig.getConfigScreen(CubeWaypointsConfig.class, parent).get();
    }
}