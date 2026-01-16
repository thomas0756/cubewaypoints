package com.amplicube.cubewaypoints.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BlockGetter {

    public static Vec3i getPos() {
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;
        if (hit != null && hit.getType() == (HitResult.Type.BLOCK)) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            return new Vec3i(pos.getX(), pos.getY(), pos.getZ());

        }
        return null;
    }
}
