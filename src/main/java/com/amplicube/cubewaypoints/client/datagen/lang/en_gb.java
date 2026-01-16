package com.amplicube.cubewaypoints.client.datagen.lang;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class en_gb extends FabricLanguageProvider {
    public en_gb(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, "en_gb", registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider wrapperLookup, FabricLanguageProvider.TranslationBuilder translationBuilder) {
        translationBuilder.add("key.category.cubewaypoints.cw_mappings", "Cube Waypoints");
        translationBuilder.add("key.cubewaypoints.primary_waypoint", "Add Primary Waypoint");
        translationBuilder.add("key.cubewaypoints.secondary_waypoint", "Add Secondary Waypoint");
        translationBuilder.add("key.cubewaypoints.remove_waypoint", "Remove Waypoint");
        translationBuilder.add("key.cubewaypoints.clear_waypoints", "Clear Waypoints");

        translationBuilder.add("text.autoconfig.cubewaypoints.title", "Cube Waypoints");
        translationBuilder.add("text.autoconfig.cubewaypoints.option.primaryColour", "Primary Waypoint Colour");
        translationBuilder.add("text.autoconfig.cubewaypoints.option.secondaryColour", "Secondary Waypoint Colour");
        translationBuilder.add("text.autoconfig.cubewaypoints.option.outlineWidth", "Outline Width");
        translationBuilder.add("text.autoconfig.cubewaypoints.option.outlineAlpha", "Outline Alpha");

    }
}
