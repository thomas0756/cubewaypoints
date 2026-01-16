package com.amplicube.cubewaypoints;

import net.minecraft.core.Vec3i;

public class CWaypoint {

    private Vec3i position;
    private FloatRGBA colour;

    public CWaypoint(int x, int y, int z, float r, float g, float b, float a){
        this.position = new Vec3i(x, y, z);
        this.colour = new FloatRGBA(r, g, b, a);
    }
    public CWaypoint(int x, int y, int z, int colour){
        this.position = new Vec3i(x, y, z);
        this.colour = new FloatRGBA((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, (colour >> 24) & 0xFF);
    }

    public boolean equals(Object o) {
        if (o instanceof CWaypoint wp) {
            return position.equals(wp.getPosition());
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return position.hashCode();
    }


    public Vec3i getPosition() {
        return this.position;
    }
    public int getX() {
        return this.position.getX();
    }
    public int getY () {
        return this.position.getY();
    }
    public int getZ () {
        return this.position.getZ();
    }
    public FloatRGBA getColour() {
        return this.colour;
    }
}
