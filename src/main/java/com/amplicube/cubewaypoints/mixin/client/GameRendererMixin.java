package com.amplicube.cubewaypoints.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;

import com.amplicube.cubewaypoints.client.render.BoxRenderer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "close", at = @At("RETURN"))
    private void onGameRendererClose(CallbackInfo ci) {
        BoxRenderer.getInstance().close();
    }
}
