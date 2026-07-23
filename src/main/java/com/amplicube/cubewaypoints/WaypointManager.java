package com.amplicube.cubewaypoints;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.HashSet;
import java.util.Set;

public class WaypointManager {
    public static Set<CWaypoint> waypoints = new HashSet<>();

    // Packed positions mirroring the waypoint set, so existence checks don't allocate.
    private static final LongOpenHashSet positions = new LongOpenHashSet();

    // Bumped on every change so the renderer knows when its cached mesh is stale.
    private static int modCount = 0;

    public static Set<CWaypoint> getWaypoints() {
        return waypoints;
    }

    public static int getModCount() {
        return modCount;
    }

    public static void addWaypoint(Vec3i pos, int colour) {
        CWaypoint toAdd =  new CWaypoint(pos.getX(), pos.getY(), pos.getZ(), colour);
        waypoints.remove(toAdd);
        waypoints.add(toAdd);
        positions.add(BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ()));
        modCount++;
    }

    public static void removeWaypoint(int x, int y, int z) {
        CWaypoint toRemove = new CWaypoint(x, y, z, 0f, 0f, 0f, 0f);
        if (waypoints.remove(toRemove)) {
            positions.remove(BlockPos.asLong(x, y, z));
            modCount++;
        }
    }

    public static void clearWaypoints() {
        if (!waypoints.isEmpty()) {
            waypoints.clear();
            positions.clear();
            modCount++;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean waypointExists(int x, int y, int z) {
        return positions.contains(BlockPos.asLong(x, y, z));
    }
}
