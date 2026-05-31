package com.rtsbuilding.rtsbuilding.client;

import java.util.List;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.blueprint.client.BlueprintPanel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(modid = RtsbuildingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class RtsBoundaryRenderer {
    private static final int GL_LEQUAL = 515;
    private static final int CHUNK_GUIDE_RADIUS_CHUNKS = 1;
    private static final int CAPTURE_BLOCK_HIGHLIGHT_LIMIT = 8192;
    private static final int CAPTURE_EXCLUDED_HIGHLIGHT_LIMIT = 1024;

    private static final RenderType CHUNK_XRAY_FILL = RenderType.create(
            "rtsbuilding_chunk_xray_fill",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLE_STRIP,
            2 * 1024 * 1024,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final RenderType CHUNK_XRAY_LINES = RenderType.create(
            "rtsbuilding_chunk_xray_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            2 * 1024 * 1024,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(RenderStateShard.DEFAULT_LINE)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final ByteBufferBuilder CHUNK_FILL_BACKING =
            new ByteBufferBuilder(CHUNK_XRAY_FILL.bufferSize());
    private static final ByteBufferBuilder CHUNK_LINE_BACKING =
            new ByteBufferBuilder(CHUNK_XRAY_LINES.bufferSize());
    private static final ByteBufferBuilder LINE_BACKING =
            new ByteBufferBuilder(RenderType.lines().bufferSize());
    private static final ByteBufferBuilder FILL_BACKING =
            new ByteBufferBuilder(RenderType.debugFilledBox().bufferSize());

    private RtsBoundaryRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        ClientRtsController controller = ClientRtsController.get();
        if (!controller.hasBounds()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            if (controller.isChunkCurtainVisible()) {
                BufferBuilder chunkFillBuffer = bufferFor(CHUNK_XRAY_FILL, CHUNK_FILL_BACKING);
                BufferBuilder chunkLineBuffer = bufferFor(CHUNK_XRAY_LINES, CHUNK_LINE_BACKING);
                renderChunkGuides(minecraft, camPos, poseStack, chunkFillBuffer, chunkLineBuffer);
                drawBuiltBufferNoDepth(CHUNK_XRAY_FILL, chunkFillBuffer);
                drawBuiltBufferNoDepth(CHUNK_XRAY_LINES, chunkLineBuffer);
            }

            RenderType lines = RenderType.lines();
            RenderType filledBox = RenderType.debugFilledBox();
            BufferBuilder lineBuffer = bufferFor(lines, LINE_BACKING);
            BufferBuilder fillBuffer = bufferFor(filledBox, FILL_BACKING);

            double ax = controller.getAnchorX();
            double ay = controller.getAnchorY();
            double az = controller.getAnchorZ();
            double r = controller.getMaxRadius();

            double minX = ax - r;
            double maxX = ax + r;
            double minZ = az - r;
            double maxZ = az + r;

            // Drag limit boundary (3 chunks radius => 48 blocks)
            LevelRenderer.renderLineBox(poseStack, lineBuffer, minX, ay - 0.25D, minZ, maxX, ay + 0.25D, maxZ,
                    1.0F, 0.25F, 0.25F, 1.0F);

            renderLinkedStorages(minecraft, controller, poseStack, lineBuffer);
            renderHoveredInteractionTarget(minecraft, controller, poseStack, lineBuffer);
            renderShapeGhostPreview(minecraft, poseStack, lineBuffer, fillBuffer);
            renderBlueprintCaptureBox(poseStack, lineBuffer, fillBuffer);
            renderBlueprintGhostPreview(minecraft, poseStack, lineBuffer, fillBuffer);

            drawBuiltBuffer(lines, lineBuffer);
            drawBuiltBuffer(filledBox, fillBuffer);
        } finally {
            poseStack.popPose();
        }
    }

    private static BufferBuilder bufferFor(RenderType renderType, ByteBufferBuilder backing) {
        return new BufferBuilder(backing, renderType.mode, renderType.format);
    }

    private static void drawBuiltBuffer(RenderType renderType, BufferBuilder buffer) {
        MeshData meshData = buffer.build();
        if (meshData != null) {
            renderType.draw(meshData);
        }
    }

    private static void drawBuiltBufferNoDepth(RenderType renderType, BufferBuilder buffer) {
        MeshData meshData = buffer.build();
        if (meshData != null) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            renderType.draw(meshData);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL_LEQUAL);
        }
    }

    private static void renderChunkGuides(
            Minecraft minecraft,
            Vec3 cameraPosition,
            PoseStack poseStack,
            VertexConsumer fillBuffer,
            VertexConsumer lineBuffer) {
        if (minecraft.level == null) {
            return;
        }
        BlockPos cameraBlockPos = BlockPos.containing(cameraPosition);
        int centerChunkX = SectionPos.blockToSectionCoord(cameraBlockPos.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(cameraBlockPos.getZ());
        int minChunkX = centerChunkX - CHUNK_GUIDE_RADIUS_CHUNKS;
        int maxChunkX = centerChunkX + CHUNK_GUIDE_RADIUS_CHUNKS;
        int minChunkZ = centerChunkZ - CHUNK_GUIDE_RADIUS_CHUNKS;
        int maxChunkZ = centerChunkZ + CHUNK_GUIDE_RADIUS_CHUNKS;
        int guideYSource = minecraft.player == null ? cameraBlockPos.getY() : minecraft.player.blockPosition().getY();
        int guideY = Mth.clamp(guideYSource, minecraft.level.getMinBuildHeight(), minecraft.level.getMaxBuildHeight() - 1);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                renderChunkEdgeHighlights(minecraft, poseStack, fillBuffer, lineBuffer, cx, cz, guideY);
            }
        }
    }

    private static void renderChunkEdgeHighlights(
            Minecraft minecraft,
            PoseStack poseStack,
            VertexConsumer fillBuffer,
            VertexConsumer lineBuffer,
            int chunkX,
            int chunkZ,
            int guideY) {
        int startX = chunkX << 4;
        int startZ = chunkZ << 4;
        int endX = startX + 15;
        int endZ = startZ + 15;
        if (!minecraft.level.hasChunkAt(new BlockPos(startX, guideY, startZ))) {
            return;
        }

        ChunkGuideColor color = chunkGuideColor(chunkX, chunkZ);
        for (int x = startX; x <= endX; x++) {
            renderChunkGuideCell(poseStack, fillBuffer, lineBuffer, x, startZ, guideY, color);
            renderChunkGuideCell(poseStack, fillBuffer, lineBuffer, x, endZ, guideY, color);
        }
        for (int z = startZ + 1; z < endZ; z++) {
            renderChunkGuideCell(poseStack, fillBuffer, lineBuffer, startX, z, guideY, color);
            renderChunkGuideCell(poseStack, fillBuffer, lineBuffer, endX, z, guideY, color);
        }
    }

    private static void renderChunkGuideCell(
            PoseStack poseStack,
            VertexConsumer fillBuffer,
            VertexConsumer lineBuffer,
            int x,
            int z,
            int guideY,
            ChunkGuideColor color) {
        double inset = 0.04D;
        double minX = x + inset;
        double minY = guideY + inset;
        double minZ = z + inset;
        double maxX = x + 1.0D - inset;
        double maxY = guideY + 1.0D - inset;
        double maxZ = z + 1.0D - inset;
        LevelRenderer.addChainedFilledBoxVertices(
                poseStack,
                fillBuffer,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                color.r(),
                color.g(),
                color.b(),
                color.a());
        LevelRenderer.renderLineBox(
                poseStack,
                lineBuffer,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                Math.min(1.0F, color.r() + 0.18F),
                Math.min(1.0F, color.g() + 0.18F),
                Math.min(1.0F, color.b() + 0.18F),
                0.92F);
    }

    private static ChunkGuideColor chunkGuideColor(int chunkX, int chunkZ) {
        return ((chunkX ^ chunkZ) & 1) == 0
                ? new ChunkGuideColor(0.16F, 0.78F, 1.0F, 0.24F)
                : new ChunkGuideColor(1.0F, 0.88F, 0.16F, 0.22F);
    }

    private record ChunkGuideColor(float r, float g, float b, float a) {
    }

    private static void renderLinkedStorages(Minecraft minecraft, ClientRtsController controller, PoseStack poseStack,
            VertexConsumer lineBuffer) {
        if (minecraft.level == null || controller.getLinkedStoragePositions().isEmpty()) {
            return;
        }

        for (BlockPos pos : controller.getLinkedStoragePositions()) {
            if (!minecraft.level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    pos.getX() - 0.002D,
                    pos.getY() - 0.002D,
                    pos.getZ() - 0.002D,
                    pos.getX() + 1.002D,
                    pos.getY() + 1.002D,
                    pos.getZ() + 1.002D,
                    0.24F, 0.55F, 1.00F, 1.0F);
        }
    }

    private static void renderHoveredInteractionTarget(Minecraft minecraft, ClientRtsController controller,
            PoseStack poseStack, VertexConsumer lineBuffer) {
        if (controller.isRotateCaptured() || minecraft.level == null || minecraft.getCameraEntity() == null) {
            return;
        }

        Vec3 camPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 viewDir = computeCursorRayDirection(minecraft);
        Vec3 to = camPos.add(viewDir.scale(128.0D));
        BlockHitResult blockHit = raycastBlockFromCursor(minecraft, camPos, to, false);
        EntityHitResult entityHit = raycastEntityFromCursor(minecraft, camPos, to, viewDir, 128.0D);
        double blockDist = blockHit != null ? camPos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double entityDist = entityHit != null ? camPos.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

        if (entityHit != null && entityDist <= blockDist) {
            Entity entity = entityHit.getEntity();
            AABB bb = entity.getBoundingBox().inflate(0.03D);
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    bb.minX,
                    bb.minY,
                    bb.minZ,
                    bb.maxX,
                    bb.maxY,
                    bb.maxZ,
                    0.35F,
                    1.0F,
                    0.55F,
                    1.0F);
            return;
        }
        if (blockHit == null || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.isAir()) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    pos.getX() + 1.0D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 1.0D,
                    1.0F, 0.95F, 0.2F, 1.0F);
            return;
        }

        var shape = state.getShape(minecraft.level, pos);
        if (shape.isEmpty()) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    pos.getX() + 1.0D,
                    pos.getY() + 1.0D,
                    pos.getZ() + 1.0D,
                    1.0F, 0.95F, 0.2F, 1.0F);
            return;
        }

        for (AABB box : shape.toAabbs()) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    pos.getX() + box.minX,
                    pos.getY() + box.minY,
                    pos.getZ() + box.minZ,
                    pos.getX() + box.maxX,
                    pos.getY() + box.maxY,
                    pos.getZ() + box.maxZ,
                    1.0F, 0.95F, 0.2F, 1.0F);
        }
    }

    private static void renderShapeGhostPreview(Minecraft minecraft, PoseStack poseStack, VertexConsumer lineBuffer,
            VertexConsumer fillBuffer) {
        if (!(minecraft.screen instanceof BuilderScreen builderScreen)) {
            return;
        }
        BuilderScreen.ShapeGhostPreview preview = builderScreen.getShapeGhostPreview();
        if (preview.blocks().isEmpty()) {
            return;
        }

        float lineR = preview.readyConfirm() ? 0.45F : 0.30F;
        float lineG = preview.readyConfirm() ? 0.95F : 0.75F;
        float lineB = preview.readyConfirm() ? 0.45F : 1.00F;
        float fillR = preview.readyConfirm() ? 0.24F : 0.16F;
        float fillG = preview.readyConfirm() ? 0.72F : 0.55F;
        float fillB = preview.readyConfirm() ? 0.24F : 0.90F;
        float fillA = preview.readyConfirm() ? 0.22F : 0.16F;

        for (BlockPos pos : preview.blocks()) {
            double minX = pos.getX() + 0.03D;
            double minY = pos.getY() + 0.03D;
            double minZ = pos.getZ() + 0.03D;
            double maxX = pos.getX() + 0.97D;
            double maxY = pos.getY() + 0.97D;
            double maxZ = pos.getZ() + 0.97D;
            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    fillR,
                    fillG,
                    fillB,
                    fillA);
        }

        for (BlockPos pos : preview.blocks()) {
            double minX = pos.getX() + 0.03D;
            double minY = pos.getY() + 0.03D;
            double minZ = pos.getZ() + 0.03D;
            double maxX = pos.getX() + 0.97D;
            double maxY = pos.getY() + 0.97D;
            double maxZ = pos.getZ() + 0.97D;
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    lineR,
                    lineG,
                    lineB,
                    0.95F);
        }
    }

    private static void renderBlueprintCaptureBox(PoseStack poseStack, VertexConsumer lineBuffer, VertexConsumer fillBuffer) {
        BlockPos first = BlueprintPanel.getCapturePointA();
        if (first == null) {
            return;
        }
        BlockPos second = BlueprintPanel.getCapturePreviewPointB();
        if (second == null) {
            second = first;
        }
        double minX = Math.min(first.getX(), second.getX()) - 0.01D;
        double minY = Math.min(first.getY(), second.getY()) + 0.99D;
        double minZ = Math.min(first.getZ(), second.getZ()) - 0.01D;
        double maxX = Math.max(first.getX(), second.getX()) + 1.01D;
        double maxY = Math.max(first.getY(), second.getY()) + 1.01D;
        double maxZ = Math.max(first.getZ(), second.getZ()) + 1.01D;
        if (minY > maxY) {
            minY = maxY - 0.02D;
        }
        List<BlockPos> includedBlocks = BlueprintPanel.getCaptureIncludedBlocksForRender(CAPTURE_BLOCK_HIGHLIGHT_LIMIT);
        if (BlueprintPanel.shouldRenderCapturePreviewFill()
                && !BlueprintPanel.shouldRenderCaptureBlockHighlights(CAPTURE_BLOCK_HIGHLIGHT_LIMIT)) {
            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    0.12F,
                    0.46F,
                    0.95F,
                    0.06F);
        }
        for (BlockPos pos : includedBlocks) {
            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    pos.getX() + 0.04D,
                    pos.getY() + 0.04D,
                    pos.getZ() + 0.04D,
                    pos.getX() + 0.96D,
                    pos.getY() + 0.96D,
                    pos.getZ() + 0.96D,
                    0.12F,
                    0.56F,
                    1.0F,
                    0.11F);
        }
        for (BlockPos pos : BlueprintPanel.getCaptureExcludedBlocksForRender(CAPTURE_EXCLUDED_HIGHLIGHT_LIMIT)) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    pos.getX() + 0.06D,
                    pos.getY() + 0.06D,
                    pos.getZ() + 0.06D,
                    pos.getX() + 0.94D,
                    pos.getY() + 0.94D,
                    pos.getZ() + 0.94D,
                    1.0F,
                    0.36F,
                    0.12F,
                    0.95F);
        }
        LevelRenderer.renderLineBox(
                poseStack,
                lineBuffer,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                0.35F,
                0.78F,
                1.0F,
                0.95F);
    }

    private static void renderBlueprintGhostPreview(Minecraft minecraft, PoseStack poseStack, VertexConsumer lineBuffer,
            VertexConsumer fillBuffer) {
        if (!(minecraft.screen instanceof BuilderScreen builderScreen)) {
            return;
        }
        BuilderScreen.BlueprintGhostPreview preview = builderScreen.getBlueprintGhostPreview();
        if (preview.blocks().isEmpty()) {
            return;
        }

        float lineR = preview.materialsReady() ? 0.35F : 1.00F;
        float lineG = preview.materialsReady() ? 0.95F : 0.72F;
        float lineB = preview.materialsReady() ? 0.72F : 0.22F;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean renderedBlockModels = false;
        MultiBufferSource.BufferSource blockBuffer = minecraft.renderBuffers().bufferSource();

        for (BlueprintPanel.BlueprintGhostBlock block : preview.blocks()) {
            BlockPos pos = block.pos();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);

            BlockState state = block.state();
            if (!block.missing()
                    && state != null
                    && !state.isAir()
                    && state.getRenderShape() == RenderShape.MODEL) {
                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                minecraft.getBlockRenderer().renderSingleBlock(
                        state,
                        poseStack,
                        blockBuffer,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
                renderedBlockModels = true;
                continue;
            }

            double cellMinX = pos.getX() + 0.04D;
            double cellMinY = pos.getY() + 0.04D;
            double cellMinZ = pos.getZ() + 0.04D;
            double cellMaxX = pos.getX() + 0.96D;
            double cellMaxY = pos.getY() + 0.96D;
            double cellMaxZ = pos.getZ() + 0.96D;
            float fallbackR = block.missing() ? 1.00F : lineR;
            float fallbackG = block.missing() ? 0.25F : lineG;
            float fallbackB = block.missing() ? 0.25F : lineB;
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    cellMinX,
                    cellMinY,
                    cellMinZ,
                    cellMaxX,
                    cellMaxY,
                    cellMaxZ,
                    fallbackR,
                    fallbackG,
                    fallbackB,
                    0.90F);
        }

        if (renderedBlockModels) {
            blockBuffer.endBatch();
        }

        if (minX != Integer.MAX_VALUE) {
            float alpha = preview.truncated() ? 0.55F : 0.75F;
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    minX - 0.02D,
                    minY - 0.02D,
                    minZ - 0.02D,
                    maxX + 0.02D,
                    maxY + 0.02D,
                    maxZ + 0.02D,
                    lineR,
                    lineG,
                    lineB,
                    alpha);
        }
    }

    private static BlockHitResult raycastBlockFromCursor(Minecraft minecraft, Vec3 camPos, Vec3 to,
            boolean includeFluidSource) {
        ClipContext.Fluid fluidMode = includeFluidSource ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE;
        HitResult hit = minecraft.level.clip(new ClipContext(camPos, to, ClipContext.Block.OUTLINE, fluidMode,
                minecraft.getCameraEntity()));
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            return bhr;
        }
        return null;
    }

    private static EntityHitResult raycastEntityFromCursor(Minecraft minecraft, Vec3 camPos, Vec3 to, Vec3 viewDir,
            double reach) {
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) {
            return null;
        }
        AABB search = cameraEntity.getBoundingBox().expandTowards(viewDir.scale(reach)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                cameraEntity,
                camPos,
                to,
                search,
                entity -> entity != null
                        && entity.isAlive()
                        && entity.isPickable()
                        && entity != cameraEntity
                        && entity != minecraft.player,
                reach * reach);
    }

    private static Vec3 computeCursorRayDirection(Minecraft minecraft) {
        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        double width = Math.max(1.0D, minecraft.getWindow().getScreenWidth());
        double height = Math.max(1.0D, minecraft.getWindow().getScreenHeight());

        double nx = (mouseX / width) * 2.0D - 1.0D;
        double ny = 1.0D - (mouseY / height) * 2.0D;

        float yawDeg = minecraft.gameRenderer.getMainCamera().getYRot();
        float pitchDeg = minecraft.gameRenderer.getMainCamera().getXRot();
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        Vec3 forward = new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)).normalize();

        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw)).normalize();
        Vec3 up = forward.cross(right).normalize();

        double fovY = Math.toRadians(minecraft.options.fov().get());
        double tanY = Math.tan(fovY * 0.5D);
        double tanX = tanY * (width / height);

        // Current yaw basis yields a left-vector here; invert X NDC to keep screen-right -> ray-right.
        return forward.add(right.scale(-nx * tanX)).add(up.scale(ny * tanY)).normalize();
    }
}
