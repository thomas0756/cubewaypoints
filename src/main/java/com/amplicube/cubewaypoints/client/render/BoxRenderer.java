package com.amplicube.cubewaypoints.client.render;


import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.amplicube.cubewaypoints.CWaypoint;
import com.amplicube.cubewaypoints.WaypointManager;

import com.amplicube.cubewaypoints.client.config.CubeWaypointsConfig;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;

import me.shedaniel.autoconfig.AutoConfig;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import com.amplicube.cubewaypoints.Cubewaypoints;

public class BoxRenderer implements ClientModInitializer {
    private static BoxRenderer instance;

    private static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Cubewaypoints.MOD_ID, "pipeline/debug_filled_box_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );

    // Depth-tested variant: skips fragments hidden behind terrain, avoiding overdraw.
    private static final RenderPipeline FILLED_DEPTH_TESTED = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Cubewaypoints.MOD_ID, "pipeline/debug_filled_box"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build()
    );

    private static final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    // How far the camera may drift from the mesh origin before the cached mesh is rebuilt.
    // The distance cull is padded by the same amount so waypoints never disappear between rebuilds.
    private static final int REBUILD_DISTANCE = 16;

    private GpuBuffer cachedVertices;
    private int cachedIndexCount;
    private int builtModCount = -1;
    private int originX, originY, originZ;
    private static boolean settingsDirty = true;

    private final Matrix4f modelView = new Matrix4f();

    public static BoxRenderer getInstance() {
        return instance;
    }

    static float edgeWidth;
    static float edgeAlpha;

    static float[][] baseFace;

    static float[][] bottomEdge;

    static float[][] topEdge;

    static float[][] rightEdge;

    static float[][] leftEdge;

    static float[][] blCorner;

    static float[][] brCorner;

    static float[][] tlCorner;

    static float[][] trCorner;

    public static void updateSettings() {
        CubeWaypointsConfig config = AutoConfig.getConfigHolder(CubeWaypointsConfig.class).getConfig();

        edgeWidth = config.outlineWidth / 16;
        edgeAlpha = config.outlineAlpha;

        settingsDirty = true;

        baseFace = new float[][] { // Z-
                {0, 0, 0},
                {0, 0, 1},
                {0, 1, 1},
                {0, 1, 0}
        };

        bottomEdge = new float[][] {
                {0, 0, edgeWidth},
                {0, 0, 1 - edgeWidth},
                {0, edgeWidth, 1 - edgeWidth},
                {0, edgeWidth, edgeWidth}
        };

        topEdge = new float[][] {
                {0, 1, 1 - edgeWidth},
                {0, 1, edgeWidth},
                {0, 1 - edgeWidth, edgeWidth},
                {0, 1 - edgeWidth, 1 - edgeWidth}
        };

        rightEdge = new float[][] {
                {0, 1 - edgeWidth, 1 - edgeWidth},
                {0, edgeWidth, 1 - edgeWidth},
                {0, edgeWidth, 1},
                {0, 1 - edgeWidth, 1}
        };

        leftEdge = new float[][] {
                {0, 1 - edgeWidth, 0},
                {0, edgeWidth, 0},
                {0, edgeWidth, edgeWidth},
                {0, 1 - edgeWidth, edgeWidth}
        };


        blCorner = new float[][] {
                {0, 0, edgeWidth},
                {0, edgeWidth, edgeWidth},
                {0, edgeWidth, 0},
                {0, 0, 0}
        };

        brCorner = new float[][] {
                {0, 0, 1},
                {0, edgeWidth, 1},
                {0, edgeWidth, 1- edgeWidth},
                {0, 0, 1 - edgeWidth}
        };

        tlCorner = new float[][] {
                {0, 1, 0},
                {0, 1 - edgeWidth, 0},
                {0, 1 - edgeWidth, edgeWidth},
                {0, 1, edgeWidth}
        };

        trCorner = new float[][] {
                {0, 1, 1 - edgeWidth},
                {0, 1 - edgeWidth, 1 - edgeWidth},
                {0, 1 - edgeWidth, 1},
                {0, 1, 1}
        };
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        WorldRenderEvents.END_MAIN.register(this::renderWaypoints);
    }

    public void renderWaypoints(WorldRenderContext context) {
        CubeWaypointsConfig config = AutoConfig.getConfigHolder(CubeWaypointsConfig.class).getConfig();
        if (!config.showWaypoints) return;

        Vec3 camera = context.worldState().cameraRenderState.pos;

        if (meshOutOfDate(camera)) rebuildMesh(config, camera);
        if (cachedVertices == null || cachedIndexCount == 0) return;

        // Vertices are baked relative to the build origin; shift them to camera space here so
        // the cached buffer stays valid while the camera moves.
        modelView.set(RenderSystem.getModelViewMatrix())
                .mul(context.matrices().last().pose())
                .translate((float) (originX - camera.x), (float) (originY - camera.y), (float) (originZ - camera.z));

        draw(Minecraft.getInstance(), config.depthTest ? FILLED_DEPTH_TESTED : FILLED_THROUGH_WALLS);
    }

    private boolean meshOutOfDate(Vec3 camera) {
        if (settingsDirty || builtModCount != WaypointManager.getModCount()) return true;

        double dx = camera.x - originX;
        double dy = camera.y - originY;
        double dz = camera.z - originZ;
        return dx * dx + dy * dy + dz * dz > (double) REBUILD_DISTANCE * REBUILD_DISTANCE;
    }

    private void rebuildMesh(CubeWaypointsConfig config, Vec3 camera) {
        settingsDirty = false;
        builtModCount = WaypointManager.getModCount();
        originX = (int) Math.floor(camera.x);
        originY = (int) Math.floor(camera.y);
        originZ = (int) Math.floor(camera.z);

        if (cachedVertices != null) {
            cachedVertices.close();
            cachedVertices = null;
        }
        cachedIndexCount = 0;

        if (WaypointManager.getWaypoints().isEmpty()) return;

        int maxDistance = config.maxRenderDistance;
        double cullDistance = maxDistance + REBUILD_DISTANCE + 1;
        double cullDistanceSq = maxDistance > 0 ? cullDistance * cullDistance : Double.MAX_VALUE;

        BufferBuilder buffer = new BufferBuilder(allocator, FILLED_THROUGH_WALLS.getVertexFormatMode(), FILLED_THROUGH_WALLS.getVertexFormat());

        for (CWaypoint waypoint : WaypointManager.getWaypoints()) {
            double dx = (waypoint.getX() + 0.5) - camera.x;
            double dy = (waypoint.getY() + 0.5) - camera.y;
            double dz = (waypoint.getZ() + 0.5) - camera.z;
            if (dx * dx + dy * dy + dz * dz > cullDistanceSq) continue;

            renderCube(buffer, waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.getColour().getR(), waypoint.getColour().getG(), waypoint.getColour().getB(), waypoint.getColour().getA());
        }

        MeshData builtBuffer = buffer.build();
        if (builtBuffer == null) return;

        cachedIndexCount = builtBuffer.drawState().indexCount();
        cachedVertices = RenderSystem.getDevice().createBuffer(() -> Cubewaypoints.MOD_ID + " waypoint vertices", GpuBuffer.USAGE_VERTEX, builtBuffer.vertexBuffer());
        builtBuffer.close();
    }


    public void drawFace(BufferBuilder buffer, boolean[] sides, boolean[] diags, int axis, int dir, float x, float y, float z, float r, float g, float b, float a) {
        // Body
        drawRect(buffer, baseFace, axis, dir, x, y, z, r, g, b, a);

        // Edges and Corners
        if (axis == 0) {
            if (sides[1]) drawRect(buffer, leftEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[2]) drawRect(buffer, bottomEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[4]) drawRect(buffer, rightEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[5]) drawRect(buffer, topEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);

            if (sides[1] || sides[2] || !diags[8]) drawRect(buffer, blCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[1] || sides[5] || !diags[9]) drawRect(buffer, tlCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[2] || sides[4] || !diags[10]) drawRect(buffer, brCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[4] || sides[5] || !diags[11]) drawRect(buffer, trCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
        }

        else if (axis == 1) {
            if (sides[2]) drawRect(buffer, leftEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[0]) drawRect(buffer, bottomEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[5]) drawRect(buffer, rightEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3]) drawRect(buffer, topEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);

            if (sides[0] || sides[2] || !diags[1]) drawRect(buffer, blCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[0] || sides[5] || !diags[3]) drawRect(buffer, brCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[2] || !diags[5]) drawRect(buffer, tlCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[5] || !diags[7]) drawRect(buffer, trCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
        }
        else if (axis == 2) {
            if (sides[0]) drawRect(buffer, leftEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[1]) drawRect(buffer, bottomEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3]) drawRect(buffer, rightEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[4]) drawRect(buffer, topEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);

            if (sides[0] || sides[1] || !diags[0]) drawRect(buffer, blCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[1] || !diags[4]) drawRect(buffer, brCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[0] || sides[4] || !diags[2]) drawRect(buffer, tlCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[4] || !diags[6]) drawRect(buffer, trCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
        }
    }

    // Axes: 0=Z, 1=X, 2=Y    Dirs: 0=-, 1=+
    public void drawRect(BufferBuilder buffer, float[][] quad,  int axis, int dir, float x, float y, float z, float r, float g, float b, float a){
        int i = (axis + 2) % 3;
        int j = (axis + 1) % 3;

        for (int vert = 0; vert < 4; vert ++) {
            int v;
            if (dir == 1) {
                v = vert;
            }
            else {
                v = 3 - vert;
            }

            float vertX = quad[v][i] + x;
            float vertY = quad[v][j] + y;
            float vertZ = quad[v][axis] + z;

            if (axis == 0) vertZ += 1f * dir;
            else if (axis == 1) vertX += 1f * dir;
            else if (axis == 2) vertY += 1f * dir;

            buffer.addVertex(vertX, vertY, vertZ).setColor(r, g, b, a);
        }
    }



    public void drawCube(BufferBuilder buffer, boolean[] sides, boolean[] diags, float x, float y, float z, float r, float g, float b, float a) {

        if (sides[0]) drawFace(buffer, sides, diags, 0, 0, x, y, z, r, g, b, a);
        if (sides[1]) drawFace(buffer, sides, diags, 1, 0, x, y, z, r, g, b, a);
        if (sides[2]) drawFace(buffer, sides, diags, 2, 0, x, y, z, r, g, b, a);
        if (sides[3]) drawFace(buffer, sides, diags, 0, 1, x, y, z, r, g, b, a);
        if (sides[4]) drawFace(buffer, sides, diags, 1, 1, x, y, z, r, g, b, a);
        if (sides[5]) drawFace(buffer, sides, diags, 2, 1, x, y, z, r, g, b, a);
    }

    private void renderCube(BufferBuilder buffer, int x, int y, int z, float r, float g, float b, float a) {

        boolean[] sidesToDraw = {
            (!WaypointManager.waypointExists(x, y, z - 1)),    // Z-    0
            (!WaypointManager.waypointExists( x - 1,  y,  z)), // X-    1
            (!WaypointManager.waypointExists( x,  y - 1,  z)), // Y-    2
            (!WaypointManager.waypointExists(x, y, z + 1)),    // Z+    3
            (!WaypointManager.waypointExists( x + 1,  y,  z)), // X+    4
            (!WaypointManager.waypointExists( x,  y + 1,  z))  // Y+    5

        };

        boolean[] diagonalsToDraw = {
            (WaypointManager.waypointExists(x - 1, y, z - 1)), // Z-, X-    0
            (WaypointManager.waypointExists(x, y - 1, z - 1)), // Z-, Y-    1
            (WaypointManager.waypointExists(x + 1, y, z - 1)), // Z-, X+    2
            (WaypointManager.waypointExists(x, y + 1, z - 1)), // Z-, Y+    3
            (WaypointManager.waypointExists(x - 1, y, z + 1)), // Z+, X-    4
            (WaypointManager.waypointExists(x, y - 1, z + 1)), // Z+, Y-    5
            (WaypointManager.waypointExists(x + 1, y, z + 1)), // Z+, X+    6
            (WaypointManager.waypointExists(x, y + 1, z + 1)), // Z+, Y+    7
            (WaypointManager.waypointExists(x - 1, y - 1, z)), // X-, Y-    8
            (WaypointManager.waypointExists(x - 1, y + 1, z)), // X-, Y+    9
            (WaypointManager.waypointExists(x + 1, y - 1, z)), // X+, Y-    10
            (WaypointManager.waypointExists(x + 1, y + 1, z)), // X+, Y+    11

        };

        boolean facesWereDrawn = false;

        for (boolean side : sidesToDraw) {
            if (side) {
                facesWereDrawn = true;
                break;
            }
        }

        if (!facesWereDrawn) return;

        drawCube(buffer, sidesToDraw, diagonalsToDraw, x - originX, y - originY, z - originZ, r, g, b, a);
    }

    private void draw(Minecraft client, RenderPipeline pipeline) {
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
        GpuBuffer indices = shapeIndexBuffer.getBuffer(cachedIndexCount);
        VertexFormat.IndexType indexType = shapeIndexBuffer.type();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> Cubewaypoints.MOD_ID + " waypoint rendering", client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);


            renderPass.setVertexBuffer(0, cachedVertices);
            renderPass.setIndexBuffer(indices, indexType);

            renderPass.drawIndexed(0, 0, cachedIndexCount, 1);
        }
    }

    public void close() {
        allocator.close();

        if (cachedVertices != null) {
            cachedVertices.close();
            cachedVertices = null;
        }
    }
}
