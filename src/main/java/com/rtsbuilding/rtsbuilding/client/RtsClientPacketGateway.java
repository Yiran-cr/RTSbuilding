package com.rtsbuilding.rtsbuilding.client;

import com.rtsbuilding.rtsbuilding.network.C2SRtsBreakPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsCameraMovePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsCraftRecipePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsFillInventoryPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsFunnelTargetPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsInteractPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsLinkStoragePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsMinePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsOpenCraftTerminalPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsOpenGuiBindingPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsPlaceFluidPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsPlacePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsQuestDetectPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsQuickDropPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsRequestCraftablesPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsRequestStoragePagePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsRotateBlockPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetAutoStorePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetFunnelPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetGuiBindingPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetModePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetQuickSlotPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsStoreFluidPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsStoreHotbarSlotPayload;
import com.rtsbuilding.rtsbuilding.network.RtsStorageSort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

final class RtsClientPacketGateway {
    private RtsClientPacketGateway() {
    }

    static void sendSetMode(BuilderMode mode) {
        PacketDistributor.sendToServer(new C2SRtsSetModePayload((byte) mode.ordinal()));
    }

    static void sendSetFunnelEnabled(boolean enabled) {
        PacketDistributor.sendToServer(new C2SRtsSetFunnelPayload(enabled));
    }

    static void sendCameraMove(float forward, float strafe, float panX, float panY, float rotateX, float rotateY,
            float scroll, int rotateSteps, boolean fast) {
        PacketDistributor.sendToServer(new C2SRtsCameraMovePayload(
                forward,
                strafe,
                panX,
                panY,
                rotateX,
                rotateY,
                scroll,
                rotateSteps,
                fast));
    }

    static void sendFunnelTarget(BlockPos target) {
        PacketDistributor.sendToServer(new C2SRtsFunnelTargetPayload(target));
    }

    static void sendLinkStorage(BlockPos pos, boolean allowStore) {
        PacketDistributor.sendToServer(new C2SRtsLinkStoragePayload(
                pos,
                allowStore ? C2SRtsLinkStoragePayload.MODE_BIDIRECTIONAL : C2SRtsLinkStoragePayload.MODE_EXTRACT_ONLY));
    }

    static void sendRequestStoragePage(int page, String search, String category, RtsStorageSort sort, boolean ascending) {
        PacketDistributor.sendToServer(new C2SRtsRequestStoragePagePayload(
                page,
                search,
                category,
                (byte) sort.ordinal(),
                ascending));
    }

    static void sendSetAutoStoreMinedDrops(boolean enabled) {
        PacketDistributor.sendToServer(new C2SRtsSetAutoStorePayload(enabled));
    }

    static void sendCraftRecipe(String recipeId, int craftCount) {
        PacketDistributor.sendToServer(new C2SRtsCraftRecipePayload(recipeId, Math.max(1, craftCount)));
    }

    static void sendOpenCraftTerminal() {
        PacketDistributor.sendToServer(new C2SRtsOpenCraftTerminalPayload());
    }

    static void sendQuestDetectManual() {
        PacketDistributor.sendToServer(new C2SRtsQuestDetectPayload(C2SRtsQuestDetectPayload.MODE_MANUAL));
    }

    static void sendRotateBlock(BlockPos pos) {
        PacketDistributor.sendToServer(new C2SRtsRotateBlockPayload(pos));
    }

    static void sendStoreHotbarSlot(int slot) {
        PacketDistributor.sendToServer(new C2SRtsStoreHotbarSlotPayload((byte) Mth.clamp(slot, 0, 8)));
    }

    static void sendFillInventory() {
        PacketDistributor.sendToServer(new C2SRtsFillInventoryPayload());
    }

    static void sendQuickDrop(String itemId, int amount, Vec3 dropPos) {
        PacketDistributor.sendToServer(new C2SRtsQuickDropPayload(
                itemId,
                (byte) Mth.clamp(amount, 1, 64),
                dropPos.x,
                dropPos.y,
                dropPos.z));
    }

    static void sendRequestCraftables(String search, boolean showUnavailable, int offset, int limit) {
        PacketDistributor.sendToServer(new C2SRtsRequestCraftablesPayload(
                search,
                showUnavailable,
                Math.max(0, offset),
                Math.max(1, limit)));
    }

    static void sendSetQuickSlot(int index, String itemId) {
        PacketDistributor.sendToServer(new C2SRtsSetQuickSlotPayload((byte) index, itemId));
    }

    static void sendSetGuiBinding(int index, BlockPos pos, Direction face, String itemIdHint) {
        PacketDistributor.sendToServer(new C2SRtsSetGuiBindingPayload(
                (byte) index,
                false,
                pos,
                (byte) (face == null ? -1 : face.get3DDataValue()),
                itemIdHint == null ? "" : itemIdHint));
    }

    static void sendClearGuiBinding(int index) {
        PacketDistributor.sendToServer(new C2SRtsSetGuiBindingPayload((byte) index, true, BlockPos.ZERO, (byte) -1, ""));
    }

    static void sendOpenGuiBinding(int index) {
        PacketDistributor.sendToServer(new C2SRtsOpenGuiBindingPayload((byte) index));
    }

    static void sendPlace(BlockHitResult hit, boolean forcePlace, boolean skipIfOccupied, String itemId, int rotateSteps,
            Vec3 rayOrigin, Vec3 rayDir) {
        PacketDistributor.sendToServer(new C2SRtsPlacePayload(
                hit.getBlockPos(),
                (byte) hit.getDirection().get3DDataValue(),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                (byte) rotateSteps,
                forcePlace,
                skipIfOccupied,
                itemId,
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    static void sendPlaceFluid(BlockHitResult hit, boolean forcePlace, String fluidId, Vec3 rayOrigin, Vec3 rayDir) {
        PacketDistributor.sendToServer(new C2SRtsPlaceFluidPayload(
                hit.getBlockPos(),
                (byte) hit.getDirection().get3DDataValue(),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                forcePlace,
                fluidId,
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    static void sendStoreFluid(byte sourceType, int toolSlot, String itemId) {
        PacketDistributor.sendToServer(new C2SRtsStoreFluidPayload(
                sourceType,
                (byte) Mth.clamp(toolSlot, 0, 8),
                itemId == null ? "" : itemId));
    }

    static void sendInteractBlockWithToolSlot(BlockHitResult hit, int toolSlot, Vec3 rayOrigin, Vec3 rayDir) {
        PacketDistributor.sendToServer(new C2SRtsInteractPayload(
                C2SRtsInteractPayload.NO_ENTITY,
                hit.getBlockPos(),
                (byte) hit.getDirection().get3DDataValue(),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                C2SRtsInteractPayload.SOURCE_TOOL_SLOT,
                (byte) Mth.clamp(toolSlot, 0, 8),
                "",
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    static void sendInteractBlockWithPinnedItem(BlockHitResult hit, String itemId, Vec3 rayOrigin, Vec3 rayDir) {
        PacketDistributor.sendToServer(new C2SRtsInteractPayload(
                C2SRtsInteractPayload.NO_ENTITY,
                hit.getBlockPos(),
                (byte) hit.getDirection().get3DDataValue(),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                C2SRtsInteractPayload.SOURCE_PIN_ITEM,
                (byte) 0,
                itemId,
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    static void sendInteractEntityWithToolSlot(int entityId, Vec3 hitLocation, int toolSlot, Vec3 rayOrigin, Vec3 rayDir) {
        PacketDistributor.sendToServer(new C2SRtsInteractPayload(
                entityId,
                BlockPos.containing(hitLocation),
                (byte) 1,
                hitLocation.x,
                hitLocation.y,
                hitLocation.z,
                C2SRtsInteractPayload.SOURCE_TOOL_SLOT,
                (byte) Mth.clamp(toolSlot, 0, 8),
                "",
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    static void sendInteractEntityWithPinnedItem(int entityId, Vec3 hitLocation, String itemId, Vec3 rayOrigin, Vec3 rayDir) {
        PacketDistributor.sendToServer(new C2SRtsInteractPayload(
                entityId,
                BlockPos.containing(hitLocation),
                (byte) 1,
                hitLocation.x,
                hitLocation.y,
                hitLocation.z,
                C2SRtsInteractPayload.SOURCE_PIN_ITEM,
                (byte) 0,
                itemId,
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    static void sendBreakPlaced(BlockPos pos, Direction face, boolean allowAdjacentFallback) {
        PacketDistributor.sendToServer(new C2SRtsBreakPayload(
                pos,
                (byte) face.get3DDataValue(),
                allowAdjacentFallback));
    }

    static void sendMineStart(BlockPos pos, int face, int toolSlot) {
        PacketDistributor.sendToServer(new C2SRtsMinePayload(pos, (byte) face, true, (byte) Mth.clamp(toolSlot, 0, 8)));
    }

    static void sendMineAbort(BlockPos pos, int face, int toolSlot) {
        PacketDistributor.sendToServer(new C2SRtsMinePayload(
                pos,
                (byte) face,
                false,
                (byte) Mth.clamp(toolSlot, 0, 8)));
    }
}
