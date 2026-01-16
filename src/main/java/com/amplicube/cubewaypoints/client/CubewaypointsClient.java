package com.amplicube.cubewaypoints.client;


import com.amplicube.cubewaypoints.Cubewaypoints;
import com.amplicube.cubewaypoints.WaypointManager;

import com.amplicube.cubewaypoints.client.config.CubeWaypointsConfig;
import com.amplicube.cubewaypoints.client.render.BoxRenderer;
import com.mojang.blaze3d.platform.InputConstants;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

public class CubewaypointsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CubeWaypointsConfig.onInitialize();
        setupKeymappings();
        BoxRenderer.updateSettings();
    }

    public void setupKeymappings() {
        KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Cubewaypoints.MOD_ID, "cw_mappings"));

        KeyMapping primaryWaypointKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.cubewaypoints.primary_waypoint", // The translation key for the key mapping.
                        InputConstants.Type.KEYSYM, // // The type of the keybinding; KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_B, // The GLFW keycode of the key.
                        CATEGORY // The category of the mapping.
                ));

        KeyMapping secondaryWaypointKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.cubewaypoints.secondary_waypoint",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_N,
                        CATEGORY
                ));

        KeyMapping removeWaypointKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.cubewaypoints.remove_waypoint",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_M,
                        CATEGORY
                ));
        KeyMapping clearWaypointsKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.cubewaypoints.clear_waypoints",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_COMMA,
                        CATEGORY
                ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            CubeWaypointsConfig config = AutoConfig.getConfigHolder(CubeWaypointsConfig.class).getConfig();
            Vec3i targetedCoords = BlockGetter.getPos();

            while (primaryWaypointKey.consumeClick()) {
                if (targetedCoords != null) {
                    WaypointManager.addWaypoint(targetedCoords, config.primaryColour);
                }
            }
            while (secondaryWaypointKey.consumeClick()) {
                if (targetedCoords != null) {
                    WaypointManager.addWaypoint(targetedCoords, config.secondaryColour);
                }
            }
            while (removeWaypointKey.consumeClick()) {
                if (targetedCoords != null) {
                    WaypointManager.removeWaypoint(targetedCoords.getX(), targetedCoords.getY(), targetedCoords.getZ());
                }
            }
            while (clearWaypointsKey.consumeClick()) {
                WaypointManager.clearWaypoints();
            }
        });
    }
}
