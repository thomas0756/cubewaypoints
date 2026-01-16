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
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import me.shedaniel.autoconfig.AutoConfig;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
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

    private static final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private BufferBuilder buffer;


    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private MappableRingBuffer vertexBuffer;

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
        for (CWaypoint waypoint : WaypointManager.getWaypoints()) {
            extractAndDrawWaypoint(context, waypoint);
        }
    }

    private void extractAndDrawWaypoint(WorldRenderContext context, CWaypoint waypoint) {
        if (renderWaypoint(context, waypoint)) drawFilledThroughWalls(Minecraft.getInstance(), FILLED_THROUGH_WALLS);
    }

    private boolean renderWaypoint(WorldRenderContext context, CWaypoint waypoint) {
        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (buffer == null) {
            buffer = new BufferBuilder(allocator, FILLED_THROUGH_WALLS.getVertexFormatMode(), FILLED_THROUGH_WALLS.getVertexFormat());
        }

        boolean facesWereRendered =  renderCube(matrices.last().pose(), buffer, waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.getColour().getR(), waypoint.getColour().getG(), waypoint.getColour().getB(), waypoint.getColour().getA());

        matrices.popPose();

        return facesWereRendered;
    }


    public void drawFace(Matrix4fc matrix, BufferBuilder buffer, boolean[] sides, boolean[] diags, int axis, int dir, float x, float y, float z, float r, float g, float b, float a) {
        // Body
        drawRect(matrix, buffer, baseFace, axis, dir, x, y, z, r, g, b, a);

        // Edges and Corners
        if (axis == 0) {
            if (sides[1]) drawRect(matrix, buffer, leftEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[2]) drawRect(matrix, buffer, bottomEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[4]) drawRect(matrix, buffer, rightEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[5]) drawRect(matrix, buffer, topEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);

            if (sides[1] || sides[2] || !diags[8]) drawRect(matrix, buffer, blCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[1] || sides[5] || !diags[9]) drawRect(matrix, buffer, tlCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[2] || sides[4] || !diags[10]) drawRect(matrix, buffer, brCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[4] || sides[5] || !diags[11]) drawRect(matrix, buffer, trCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
        }

        else if (axis == 1) {
            if (sides[2]) drawRect(matrix, buffer, leftEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[0]) drawRect(matrix, buffer, bottomEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[5]) drawRect(matrix, buffer, rightEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3]) drawRect(matrix, buffer, topEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);

            if (sides[0] || sides[2] || !diags[1]) drawRect(matrix, buffer, blCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[0] || sides[5] || !diags[3]) drawRect(matrix, buffer, brCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[2] || !diags[5]) drawRect(matrix, buffer, tlCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[5] || !diags[7]) drawRect(matrix, buffer, trCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
        }
        else if (axis == 2) {
            if (sides[0]) drawRect(matrix, buffer, leftEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[1]) drawRect(matrix, buffer, bottomEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3]) drawRect(matrix, buffer, rightEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[4]) drawRect(matrix, buffer, topEdge, axis, dir, x, y, z, r, g, b, edgeAlpha);

            if (sides[0] || sides[1] || !diags[0]) drawRect(matrix, buffer, blCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[1] || !diags[4]) drawRect(matrix, buffer, brCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[0] || sides[4] || !diags[2]) drawRect(matrix, buffer, tlCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
            if (sides[3] || sides[4] || !diags[6]) drawRect(matrix, buffer, trCorner, axis, dir, x, y, z, r, g, b, edgeAlpha);
        }
    }

    // Axes: 0=Z, 1=X, 2=Y    Dirs: 0=-, 1=+
    public void drawRect(Matrix4fc matrix, BufferBuilder buffer, float[][] quad,  int axis, int dir, float x, float y, float z, float r, float g, float b, float a){
        int i = (axis + 2) % 3;
        int j = (axis + 1) % 3;
        @SuppressWarnings("unused")
        int k = axis;

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
            float vertZ = quad[v][k] + z;

            if (axis == 0) vertZ += 1f * dir;
            else if (axis == 1) vertX += 1f * dir;
            else if (axis == 2) vertY += 1f * dir;

            buffer.addVertex(matrix, vertX, vertY, vertZ).setColor(r, g, b, a);
        }
    }



    public void drawCube(Matrix4fc matrix, BufferBuilder buffer, boolean[] sides, boolean[] diags, float x, float y, float z, float r, float g, float b, float a) {

        if (sides[0]) drawFace(matrix, buffer, sides, diags, 0, 0, x, y, z, r, g, b, a);
        if (sides[1]) drawFace(matrix, buffer, sides, diags, 1, 0, x, y, z, r, g, b, a);
        if (sides[2]) drawFace(matrix, buffer, sides, diags, 2, 0, x, y, z, r, g, b, a);
        if (sides[3]) drawFace(matrix, buffer, sides, diags, 0, 1, x, y, z, r, g, b, a);
        if (sides[4]) drawFace(matrix, buffer, sides, diags, 1, 1, x, y, z, r, g, b, a);
        if (sides[5]) drawFace(matrix, buffer, sides, diags, 2, 1, x, y, z, r, g, b, a);
    }

    private boolean renderCube(Matrix4fc positionMatrix, BufferBuilder buffer, float x, float y, float z, float r, float g, float b, float a) {

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

        if (!facesWereDrawn) return false;

        drawCube(positionMatrix, buffer, sidesToDraw, diagonalsToDraw, x, y, z, r, g, b, a);

        return true;
    }

    private void drawFilledThroughWalls(Minecraft client, @SuppressWarnings("SameParameterValue") RenderPipeline pipeline) {
        // Build the buffer
        MeshData builtBuffer = buffer.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);

        draw(client, pipeline, builtBuffer, drawParameters, vertices, format);

        vertexBuffer.rotate();
        buffer = null;
    }

    private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {

        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            vertexBuffer = new MappableRingBuffer(() -> Cubewaypoints.MOD_ID + " waypoint buffer", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        return vertexBuffer.currentBuffer();
    }

    private static void draw(Minecraft client, RenderPipeline pipeline, MeshData builtBuffer, MeshData.DrawState drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {

            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());

            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> Cubewaypoints.MOD_ID + " waypoint rendering", client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);


            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            //noinspection ConstantValue
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    public void close() {
        allocator.close();

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}