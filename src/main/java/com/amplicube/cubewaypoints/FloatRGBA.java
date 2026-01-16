package com.amplicube.cubewaypoints;

import static java.lang.Math.clamp;

public class FloatRGBA {
    private float r;
    private float g;
    private float b;
    private float a;

    public FloatRGBA(float r, float g, float b, float a) {
        this.r = clamp(r, 0f, 1f);
        this.g = clamp(g, 0f, 1f);
        this.b = clamp(b, 0f, 1f);
        this.a = clamp(a, 0f, 1f);
    }
    public FloatRGBA(int r, int g, int b, int a) {
        this.r = r / 255f;
        this.g = g / 255f;
        this.b = b / 255f;
        this.a = a / 255f;
    }

    public float getA() {
        return a;
    }

    public void setA(float a) {
        this.a = a;
    }

    public float getB() {
        return b;
    }

    public void setB(float b) {
        this.b = b;
    }

    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }

    public float getR() {
        return r;
    }

    public void setR(float r) {
        this.r = r;
    }
}
