package com.rtsbuilding.rtsbuilding.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.mojang.blaze3d.platform.InputConstants;
import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.network.C2SRtsBreakPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsFunnelTargetPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsCraftRecipePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsFillInventoryPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsInteractPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsLinkStoragePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsMinePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsOpenCraftTerminalPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsPlacePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsPlaceFluidPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsQuestDetectPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsQuickDropPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsRotateBlockPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsOpenGuiBindingPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsRequestCraftablesPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsStoreFluidPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsStoreHotbarSlotPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetGuiBindingPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetQuickSlotPayload;
import com.rtsbuilding.rtsbuilding.entity.RtsCameraEntity;
import com.rtsbuilding.rtsbuilding.network.C2SRtsCameraMovePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsRequestStoragePagePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetAutoStorePayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetFunnelPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsSetModePayload;
import com.rtsbuilding.rtsbuilding.network.RtsStorageSort;
import com.rtsbuilding.rtsbuilding.network.S2CRtsCameraStatePayload;
import com.rtsbuilding.rtsbuilding.network.S2CRtsCraftablesPayload;
import com.rtsbuilding.rtsbuilding.network.S2CRtsCraftFeedbackPayload;
import com.rtsbuilding.rtsbuilding.network.S2CRtsMineProgressPayload;
import com.rtsbuilding.rtsbuilding.network.S2CRtsRemoteMenuHintPayload;
import com.rtsbuilding.rtsbuilding.network.S2CRtsStoragePagePayload;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

public final class ClientRtsController {
    private static final ClientRtsController INSTANCE = new ClientRtsController();

    private static final float ROT_INPUT_CLAMP = 20.0F;
    private static final float ROTATE_GAIN_X = 0.24F;
    private static final float ROTATE_GAIN_Y = 0.22F;
    private static final float ROT_SENS_MIN = 1.00F;
    private static final float ROT_SENS_MAX = 10.00F;
    private static final float ROT_SENS_STEP = 0.50F;
    private static final double DOLLY_PER_SCROLL = 2.6D;
    private static final float[] INPUT_SENS_PRESETS = new float[] { 0.50F, 0.75F, 1.00F, 1.25F, 1.50F, 2.00F };
    private static final int INPUT_SENS_DEFAULT_INDEX = 2;
    private static final int QUICK_SLOT_COUNT = 27;
    private static final int GUI_BINDING_SLOT_COUNT = 8;
    private static final int CRAFTABLE_BATCH_SIZE = 12;
    private static final String CATEGORY_ALL = "all";
    private static final String CATEGORY_MOD_PREFIX = "mod|";
    private static final String CATEGORY_TAB_PREFIX = "tab|";

    private static final float ROT_EMA_ALPHA = 0.28F;
    private static final float ROT_EMA_DECAY = 0.78F;
    private static final int RTS_MINE_RENDER_ID = 0x525453;
    private static final int REMOTE_MENU_OPEN_GRACE_TICKS = 20;
    private static final double MIN_CAMERA_HEIGHT_OFFSET = -5.0D;
    private static final double MAX_CAMERA_HEIGHT_OFFSET = 80.0D;
    private static final float MIN_CAMERA_PITCH = -90.0F;
    private static final float MAX_CAMERA_PITCH = 90.0F;

    private boolean enabled;
    private int serverCameraEntityId = -1;

    private Entity previousCameraEntity;
    private CameraType previousCameraType = CameraType.FIRST_PERSON;
    private boolean previousBobView = true;
    private double previousFovEffectScale = 1.0D;

    private boolean rotateCaptured;
    private double restoreCursorX;
    private double restoreCursorY;

    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private double maxRadius;

    private boolean localStateReady;
    private double localX;
    private double localY;
    private double localZ;
    private double localHeightOffset;
    private float localYawDeg;
    private float localPitchDeg;

    private float pendingPanX;
    private float pendingPanY;
    private float pendingScroll;
    private int pendingRotateSteps;

    private float pendingRawRotateX;
    private float pendingRawRotateY;
    private float emaRotateX;
    private float emaRotateY;

    private float rotateSensitivity = 5.00F;
    private int inputSensitivityIndex = INPUT_SENS_DEFAULT_INDEX;

    private BuilderMode mode = BuilderMode.INTERACT;
    private boolean storageCollapsed;
    private boolean storageLinked;
    private String linkedStorageName = "No Storage";
    private final List<BlockPos> linkedStoragePositions = new ArrayList<>();
    private int storagePage;
    private int storageTotalPages = 1;
    private int storageTotalEntries;
    private int storageRevision;
    private String storageSearch = "";
    private String storageCategory = "all";
    private RtsStorageSort storageSort = RtsStorageSort.QUANTITY;
    private boolean storageSortAscending;
    private final List<String> storageCategories = new ArrayList<>();
    private final List<StorageEntry> storageEntries = new ArrayList<>();
    private final Map<String, Long> storageTotalCounts = new HashMap<>();
    private final List<FluidEntry> fluidEntries = new ArrayList<>();
    private final List<RecentEntry> recentEntries = new ArrayList<>();
    private String craftablesSearch = "";
    private boolean craftablesShowUnavailable;
    private final List<CraftableEntry> craftableEntries = new ArrayList<>();
    private int craftablesRevision;
    private boolean craftablesHasMore;
    private final Set<Integer> pendingCraftableOffsets = new HashSet<>();
    private String craftFeedbackItemId = "";
    private int craftFeedbackCount;
    private long craftFeedbackExpiryMs;
    private final List<CraftFeedbackIngredient> craftFeedbackIngredients = new ArrayList<>();
    private String selectedItemId = "";
    private String selectedItemLabel = "";
    private ItemStack selectedItemPreview = ItemStack.EMPTY;
    private String selectedFluidId = "";
    private String selectedFluidLabel = "";
    private ItemStack selectedFluidPreview = ItemStack.EMPTY;
    private int placeRotateSteps;
    private BlockPos activeMinePos;
    private int activeMineFace = -1;
    private int activeMineToolSlot;
    private BlockPos mineRenderPos;
    private BuildShape buildShape = BuildShape.BLOCK;
    private boolean pendingCraftTerminalOpen;
    private int pendingCraftTerminalOpenTicks;
    private int pendingRemoteMenuOpenTicks;
    private BlockPos pendingRemoteMenuValidationPos;
    private BlockPos activeRemoteMenuValidationPos;
    private Vec3 remoteMenuRestorePos;
    private boolean remoteMenuValidationSpoofed;
    private AbstractContainerMenu relaxedRemoteMenu;
    private boolean autoStoreMinedDrops = true;
    private final String[] quickSlotItemIds = new String[QUICK_SLOT_COUNT];
    private final String[] quickSlotLabels = new String[QUICK_SLOT_COUNT];
    private final ItemStack[] quickSlotPreviews = new ItemStack[QUICK_SLOT_COUNT];
    private final String[] guiBindingLabels = new String[GUI_BINDING_SLOT_COUNT];
    private boolean funnelEnabled;
    private BlockPos lastFunnelTarget;
    private int funnelTargetCooldownTicks;
    private final List<FunnelBufferEntry> funnelBufferEntries = new ArrayList<>();
    private double storagePanelXNormalized;
    private double storagePanelYNormalized;
    private double storagePanelWidthNormalized;
    private double storagePanelHeightNormalized;

    // Local render-only camera entity to isolate rendering from network interpolation.
    private RtsCameraEntity localMirrorCamera;

    private ClientRtsController() {
        applyStoredLayout(RtsClientLayoutStore.loadStoragePanelLayout());
        this.storageCategories.add("all");
        for (int i = 0; i < QUICK_SLOT_COUNT; i++) {
            this.quickSlotItemIds[i] = "";
            this.quickSlotLabels[i] = "";
            this.quickSlotPreviews[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < GUI_BINDING_SLOT_COUNT; i++) {
            this.guiBindingLabels[i] = "";
        }
    }

    public static ClientRtsController get() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canUseStorageOverlay() {
        return this.enabled
                || this.storageLinked
                || !this.linkedStoragePositions.isEmpty()
                || !this.storageEntries.isEmpty()
                || !this.fluidEntries.isEmpty();
    }

    public double getAnchorX() {
        return anchorX;
    }

    public double getAnchorY() {
        return anchorY;
    }

    public double getAnchorZ() {
        return anchorZ;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    public boolean hasBounds() {
        return enabled && maxRadius > 0.0D;
    }

    public BuilderMode getMode() {
        return this.mode;
    }

    public void setMode(BuilderMode mode) {
        this.mode = mode;
        PacketDistributor.sendToServer(new C2SRtsSetModePayload((byte) mode.ordinal()));
    }

    public boolean isFunnelEnabled() {
        return this.funnelEnabled;
    }

    public void setFunnelEnabled(boolean enabled) {
        if (this.funnelEnabled == enabled) {
            return;
        }
        this.funnelEnabled = enabled;
        PacketDistributor.sendToServer(new C2SRtsSetFunnelPayload(enabled));
        if (!enabled) {
            this.lastFunnelTarget = null;
            this.funnelTargetCooldownTicks = 0;
        }
    }

    public void toggleFunnelEnabled() {
        setFunnelEnabled(!this.funnelEnabled);
    }

    public boolean isStorageCollapsed() {
        return this.storageCollapsed;
    }

    public void toggleStorageCollapsed() {
        this.storageCollapsed = !this.storageCollapsed;
    }

    public double getStoragePanelXNormalized() {
        return this.storagePanelXNormalized;
    }

    public double getStoragePanelYNormalized() {
        return this.storagePanelYNormalized;
    }

    public double getStoragePanelWidthNormalized() {
        return this.storagePanelWidthNormalized;
    }

    public double getStoragePanelHeightNormalized() {
        return this.storagePanelHeightNormalized;
    }

    public void updateStoragePanelLayout(double xNormalized, double yNormalized, double widthNormalized, double heightNormalized) {
        this.storagePanelXNormalized = clampLayoutNormalized(xNormalized);
        this.storagePanelYNormalized = clampLayoutNormalized(yNormalized);
        this.storagePanelWidthNormalized = clampLayoutNormalized(widthNormalized);
        this.storagePanelHeightNormalized = clampLayoutNormalized(heightNormalized);
        RtsClientLayoutStore.saveStoragePanelLayout(new RtsClientLayoutStore.StoragePanelLayout(
                this.storagePanelXNormalized,
                this.storagePanelYNormalized,
                this.storagePanelWidthNormalized,
                this.storagePanelHeightNormalized));
    }

    public boolean isStorageLinked() {
        return this.storageLinked;
    }

    public String getLinkedStorageName() {
        return this.linkedStorageName;
    }

    public List<BlockPos> getLinkedStoragePositions() {
        return Collections.unmodifiableList(this.linkedStoragePositions);
    }

    public int getStoragePage() {
        return this.storagePage;
    }

    public int getStorageTotalPages() {
        return this.storageTotalPages;
    }

    public int getStorageTotalEntries() {
        return this.storageTotalEntries;
    }

    public int getStorageRevision() {
        return this.storageRevision;
    }

    public String getStorageSearch() {
        return this.storageSearch;
    }

    public RtsStorageSort getStorageSort() {
        return this.storageSort;
    }

    public boolean isStorageSortAscending() {
        return this.storageSortAscending;
    }

    public String getStorageCategory() {
        return this.storageCategory;
    }

    public List<String> getStorageCategories() {
        return Collections.unmodifiableList(this.storageCategories);
    }

    public String getSelectedItemId() {
        return this.selectedItemId;
    }

    public String getSelectedItemLabel() {
        return this.selectedItemLabel;
    }

    public String getSelectedFluidId() {
        return this.selectedFluidId;
    }

    public String getSelectedFluidLabel() {
        return this.selectedFluidLabel;
    }

    public boolean hasSelectedItem() {
        return !this.selectedItemId.isBlank();
    }

    public boolean hasSelectedFluid() {
        return !this.selectedFluidId.isBlank();
    }

    public ItemStack getSelectedItemPreview() {
        return this.selectedItemPreview;
    }

    public ItemStack getSelectedFluidPreview() {
        return this.selectedFluidPreview;
    }

    public int getPlaceRotateDegrees() {
        return this.placeRotateSteps * 90;
    }

    public List<StorageEntry> getStorageEntries() {
        return Collections.unmodifiableList(this.storageEntries);
    }

    public long getStorageTotalCount(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0L;
        }
        return Math.max(0L, this.storageTotalCounts.getOrDefault(itemId, 0L));
    }

    public List<FluidEntry> getFluidEntries() {
        return Collections.unmodifiableList(this.fluidEntries);
    }

    public List<RecentEntry> getRecentEntries() {
        return Collections.unmodifiableList(this.recentEntries);
    }

    public long getRecentDisplayAmount(RecentEntry entry) {
        if (entry == null) {
            return 0L;
        }
        if (entry.fluid()) {
            return getStorageFluidAmount(entry.id());
        }
        return getStorageTotalCount(entry.id());
    }

    public String getCraftablesSearch() {
        return this.craftablesSearch;
    }

    public boolean isCraftablesShowUnavailable() {
        return this.craftablesShowUnavailable;
    }

    public List<CraftableEntry> getCraftableEntries() {
        return Collections.unmodifiableList(this.craftableEntries);
    }

    public int getCraftablesRevision() {
        return this.craftablesRevision;
    }

    public boolean hasMoreCraftables() {
        return this.craftablesHasMore;
    }

    public String getCraftFeedbackItemId() {
        return this.craftFeedbackItemId;
    }

    public int getCraftFeedbackCount() {
        return this.craftFeedbackCount;
    }

    public long getCraftFeedbackExpiryMs() {
        return this.craftFeedbackExpiryMs;
    }

    public List<CraftFeedbackIngredient> getCraftFeedbackIngredients() {
        return Collections.unmodifiableList(this.craftFeedbackIngredients);
    }

    public List<FunnelBufferEntry> getFunnelBufferEntries() {
        return Collections.unmodifiableList(this.funnelBufferEntries);
    }

    public boolean isAutoStoreMinedDrops() {
        return this.autoStoreMinedDrops;
    }

    public BuildShape getBuildShape() {
        return this.buildShape;
    }

    public void setBuildShape(BuildShape shape) {
        this.buildShape = shape == null ? BuildShape.BLOCK : shape;
    }

    public void cycleBuildShape(int step) {
        BuildShape[] values = BuildShape.values();
        int index = this.buildShape.ordinal();
        int next = Math.floorMod(index + step, values.length);
        this.buildShape = values[next];
    }

    public int getQuickSlotCount() {
        return QUICK_SLOT_COUNT;
    }

    public String getQuickSlotItemId(int index) {
        if (index < 0 || index >= QUICK_SLOT_COUNT) {
            return "";
        }
        return this.quickSlotItemIds[index];
    }

    public String getQuickSlotLabel(int index) {
        if (index < 0 || index >= QUICK_SLOT_COUNT) {
            return "";
        }
        return this.quickSlotLabels[index];
    }

    public ItemStack getQuickSlotPreview(int index) {
        if (index < 0 || index >= QUICK_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return this.quickSlotPreviews[index];
    }

    public float getRotateSensitivity() {
        return this.rotateSensitivity;
    }

    public int getGuiBindingCount() {
        return GUI_BINDING_SLOT_COUNT;
    }

    public String getGuiBindingLabel(int index) {
        if (index < 0 || index >= GUI_BINDING_SLOT_COUNT) {
            return "";
        }
        return this.guiBindingLabels[index];
    }

    public boolean hasGuiBinding(int index) {
        return !getGuiBindingLabel(index).isBlank();
    }

    public String getInputSensitivityLabel() {
        return String.format(Locale.ROOT, "x%.2f", getInputSensitivityScale());
    }

    public void cycleInputSensitivity() {
        this.inputSensitivityIndex = (this.inputSensitivityIndex + 1) % INPUT_SENS_PRESETS.length;
    }

    public void increaseRotateSensitivity() {
            this.rotateSensitivity = Mth.clamp(this.rotateSensitivity + ROT_SENS_STEP, ROT_SENS_MIN, ROT_SENS_MAX);
    }

    public void decreaseRotateSensitivity() {
            this.rotateSensitivity = Mth.clamp(this.rotateSensitivity - ROT_SENS_STEP, ROT_SENS_MIN, ROT_SENS_MAX);
    }

    public void beginRotateCapture(double cursorX, double cursorY) {
        if (this.rotateCaptured) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        this.rotateCaptured = true;
        this.restoreCursorX = cursorX;
        this.restoreCursorY = cursorY;
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    public void endRotateCapture(double fallbackX, double fallbackY) {
        if (!this.rotateCaptured) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        this.rotateCaptured = false;
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        double x = this.restoreCursorX == 0.0D ? fallbackX : this.restoreCursorX;
        double y = this.restoreCursorY == 0.0D ? fallbackY : this.restoreCursorY;
        GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x, y);
    }

    public boolean isRotateCaptured() {
        return this.rotateCaptured;
    }

    public void applyServerCameraState(S2CRtsCameraStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();

        if (payload.enabled()) {
            boolean freshEnable = !this.enabled;
            this.enabled = true;
            this.serverCameraEntityId = payload.cameraEntityId();
            this.anchorX = payload.anchorX();
            this.anchorY = payload.anchorY();
            this.anchorZ = payload.anchorZ();
            this.maxRadius = payload.maxRadius();

            if (freshEnable) {
                this.previousCameraEntity = minecraft.getCameraEntity();
                this.previousCameraType = minecraft.options.getCameraType();
                this.previousBobView = minecraft.options.bobView().get();
                this.previousFovEffectScale = minecraft.options.fovEffectScale().get();
            }

            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            minecraft.options.bobView().set(false);
            minecraft.options.fovEffectScale().set(0.0D);

            if (!(minecraft.screen instanceof BuilderScreen)) {
                minecraft.setScreen(new BuilderScreen(this));
            }

            this.localHeightOffset = payload.heightOffset();
            this.localYawDeg = payload.yawDeg();
            this.localPitchDeg = payload.pitchDeg();
            this.localX = payload.anchorX();
            this.localY = payload.anchorY() + payload.heightOffset();
            this.localZ = payload.anchorZ();
            this.localStateReady = true;

            this.pendingPanX = 0.0F;
            this.pendingPanY = 0.0F;
            this.pendingScroll = 0.0F;
            this.pendingRotateSteps = 0;
            this.pendingRawRotateX = 0.0F;
            this.pendingRawRotateY = 0.0F;
            this.emaRotateX = 0.0F;
            this.emaRotateY = 0.0F;
            this.mode = BuilderMode.INTERACT;
            this.storageCollapsed = false;
            this.storageEntries.clear();
            this.fluidEntries.clear();
            this.storageLinked = false;
            this.linkedStorageName = "No Storage";
            this.linkedStoragePositions.clear();
            this.storagePage = 0;
            this.storageTotalPages = 1;
            this.storageTotalEntries = 0;
            this.storageSearch = "";
            this.storageCategory = "all";
            this.storageSort = RtsStorageSort.QUANTITY;
            this.storageSortAscending = false;
            this.inputSensitivityIndex = INPUT_SENS_DEFAULT_INDEX;
            this.storageCategories.clear();
            this.storageCategories.add("all");
            this.selectedItemId = "";
            this.selectedItemLabel = "";
            this.selectedItemPreview = ItemStack.EMPTY;
            this.selectedFluidId = "";
            this.selectedFluidLabel = "";
            this.selectedFluidPreview = ItemStack.EMPTY;
            this.placeRotateSteps = 0;
            this.activeMinePos = null;
            this.activeMineFace = -1;
            this.mineRenderPos = null;
            this.buildShape = BuildShape.BLOCK;
            this.funnelEnabled = false;
            this.lastFunnelTarget = null;
            this.funnelTargetCooldownTicks = 0;
            this.funnelBufferEntries.clear();
            this.pendingCraftTerminalOpen = false;
            this.pendingCraftTerminalOpenTicks = 0;
            this.pendingRemoteMenuOpenTicks = 0;
            clearRemoteMenuValidationState();
            clearQuickSlotsLocal();
            clearGuiBindingsLocal();

            this.ensureLocalMirrorCamera(minecraft);
            this.syncVisualCameraFrame();
            requestStoragePage(0);
            return;
        }

        this.enabled = false;
        this.serverCameraEntityId = -1;
        this.localStateReady = false;
        this.funnelEnabled = false;
        this.lastFunnelTarget = null;
        this.funnelTargetCooldownTicks = 0;
        this.funnelBufferEntries.clear();
        this.pendingCraftTerminalOpen = false;
        this.pendingCraftTerminalOpenTicks = 0;
        this.pendingRemoteMenuOpenTicks = 0;
        clearRemoteMenuValidationState();

        if (this.rotateCaptured) {
            this.endRotateCapture(0.0D, 0.0D);
        }

        this.pendingPanX = 0.0F;
        this.pendingPanY = 0.0F;
        this.pendingScroll = 0.0F;
        this.pendingRotateSteps = 0;
        this.pendingRawRotateX = 0.0F;
        this.pendingRawRotateY = 0.0F;
        this.emaRotateX = 0.0F;
        this.emaRotateY = 0.0F;
        this.inputSensitivityIndex = INPUT_SENS_DEFAULT_INDEX;
        this.selectedItemId = "";
        this.selectedItemLabel = "";
        this.selectedItemPreview = ItemStack.EMPTY;
        this.selectedFluidId = "";
        this.selectedFluidLabel = "";
        this.selectedFluidPreview = ItemStack.EMPTY;
        this.placeRotateSteps = 0;
        this.activeMinePos = null;
        this.activeMineFace = -1;
        this.buildShape = BuildShape.BLOCK;
        clearQuickSlotsLocal();
        clearGuiBindingsLocal();
        if (minecraft.level != null && this.mineRenderPos != null) {
            minecraft.level.destroyBlockProgress(RTS_MINE_RENDER_ID, this.mineRenderPos, -1);
        }
        this.mineRenderPos = null;

        if (minecraft.screen instanceof BuilderScreen) {
            minecraft.setScreen(null);
        }

        Entity restore = this.previousCameraEntity != null ? this.previousCameraEntity : minecraft.player;
        minecraft.setCameraEntity(restore);
        minecraft.options.setCameraType(this.previousCameraType);
        minecraft.options.bobView().set(this.previousBobView);
        minecraft.options.fovEffectScale().set(this.previousFovEffectScale);

        this.previousCameraEntity = null;
        this.localMirrorCamera = null;
    }

    public void preTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clearRemoteMenuValidationState();
            return;
        }
        if (!this.enabled) {
            restoreRemoteMenuValidationPosition(minecraft);
            clearRemoteMenuValidationState();
            return;
        }

        boolean hasRemoteMenuOpen = minecraft.player.containerMenu != null
                && minecraft.player.containerMenu.containerId != 0;
        if (!hasRemoteMenuOpen && this.pendingRemoteMenuOpenTicks <= 0) {
            restoreRemoteMenuValidationPosition(minecraft);
            this.activeRemoteMenuValidationPos = null;
            return;
        }

        if (this.activeRemoteMenuValidationPos == null && this.pendingRemoteMenuValidationPos != null) {
            this.activeRemoteMenuValidationPos = this.pendingRemoteMenuValidationPos.immutable();
        }
        if (this.activeRemoteMenuValidationPos == null || this.remoteMenuValidationSpoofed) {
            return;
        }

        this.remoteMenuRestorePos = minecraft.player.position();
        Vec3 validationPos = resolveMenuValidationPosition(this.activeRemoteMenuValidationPos);
        minecraft.player.setPos(validationPos.x, validationPos.y, validationPos.z);
        this.remoteMenuValidationSpoofed = true;
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        restoreRemoteMenuValidationPosition(minecraft);
        if (!this.enabled) {
            return;
        }

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (this.funnelTargetCooldownTicks > 0) {
            this.funnelTargetCooldownTicks--;
        }

        boolean hasRemoteMenuOpen = minecraft.player.containerMenu != null
                && minecraft.player.containerMenu.containerId != 0;

        if (this.pendingCraftTerminalOpen
                && minecraft.player.containerMenu instanceof CraftingMenu pendingMenu
                && minecraft.player.containerMenu.containerId != 0
                && !(minecraft.screen instanceof RtsCraftTerminalScreen)) {
            Component pendingTitle = minecraft.screen != null ? minecraft.screen.getTitle() : Component.literal("RTS Craft Terminal");
            minecraft.setScreen(new RtsCraftTerminalScreen(pendingMenu, minecraft.player.getInventory(), pendingTitle));
            this.pendingCraftTerminalOpen = false;
            this.pendingCraftTerminalOpenTicks = 0;
        }

        if (minecraft.screen instanceof CraftingScreen craftingScreen
                && minecraft.player != null
                && craftingScreen.getMenu() instanceof CraftingMenu craftingMenu
                && !(minecraft.screen instanceof RtsCraftTerminalScreen)
                && shouldUseRtsCraftTerminalScreen(craftingScreen)) {
            minecraft.setScreen(new RtsCraftTerminalScreen(craftingMenu, minecraft.player.getInventory(), craftingScreen.getTitle()));
            this.pendingCraftTerminalOpen = false;
            this.pendingCraftTerminalOpenTicks = 0;
        } else if (this.pendingCraftTerminalOpen) {
            if (this.pendingCraftTerminalOpenTicks > 0) {
                this.pendingCraftTerminalOpenTicks--;
            } else {
                this.pendingCraftTerminalOpen = false;
            }
        }

        if (hasRemoteMenuOpen) {
            this.pendingRemoteMenuOpenTicks = 0;
            if (this.relaxedRemoteMenu != minecraft.player.containerMenu) {
                relaxClientMenuValidation(minecraft.player.containerMenu);
                this.relaxedRemoteMenu = minecraft.player.containerMenu;
            }
            if (this.activeRemoteMenuValidationPos == null && this.pendingRemoteMenuValidationPos != null) {
                this.activeRemoteMenuValidationPos = this.pendingRemoteMenuValidationPos.immutable();
            }
            if (minecraft.screen instanceof BuilderScreen) {
                // First-open GUI construction can leave a brief null-screen handoff. Once a real
                // container menu exists, let it take over instead of keeping BuilderScreen active.
                minecraft.setScreen(null);
            }
        } else if (this.pendingRemoteMenuOpenTicks > 0) {
            this.pendingRemoteMenuOpenTicks--;
            if (this.activeRemoteMenuValidationPos == null && this.pendingRemoteMenuValidationPos != null) {
                this.activeRemoteMenuValidationPos = this.pendingRemoteMenuValidationPos.immutable();
            }
        } else {
            this.activeRemoteMenuValidationPos = null;
            this.pendingRemoteMenuValidationPos = null;
            this.relaxedRemoteMenu = null;
        }

        if (minecraft.screen == null && !hasRemoteMenuOpen && this.pendingRemoteMenuOpenTicks <= 0) {
            minecraft.setScreen(new BuilderScreen(this));
        }

        this.ensureLocalMirrorCamera(minecraft);

        long window = minecraft.getWindow().getWindow();
        boolean suppressMoveKeys = minecraft.screen instanceof BuilderScreen builderScreen && builderScreen.isSearchFocused();
        boolean w = !suppressMoveKeys
                && (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_W) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP));
        boolean s = !suppressMoveKeys
                && (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_S) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN));
        boolean a = !suppressMoveKeys
                && (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_A) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT));
        boolean d = !suppressMoveKeys
                && (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_D) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT));
        boolean shift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

        float forward = (w ? 1.0F : 0.0F) - (s ? 1.0F : 0.0F);
        float strafe = (a ? 1.0F : 0.0F) - (d ? 1.0F : 0.0F);

        float safeRawX = Mth.clamp(this.pendingRawRotateX, -ROT_INPUT_CLAMP, ROT_INPUT_CLAMP);
        float safeRawY = Mth.clamp(this.pendingRawRotateY, -ROT_INPUT_CLAMP, ROT_INPUT_CLAMP);

        this.emaRotateX += (safeRawX - this.emaRotateX) * ROT_EMA_ALPHA;
        this.emaRotateY += (safeRawY - this.emaRotateY) * ROT_EMA_ALPHA;

        if (Math.abs(safeRawX) < 0.0001F) {
            this.emaRotateX *= ROT_EMA_DECAY;
        }
        if (Math.abs(safeRawY) < 0.0001F) {
            this.emaRotateY *= ROT_EMA_DECAY;
        }

        float inputSensScale = getInputSensitivityScale();
        float rotateXForTick = Mth.clamp(this.emaRotateX * this.rotateSensitivity * inputSensScale, -ROT_INPUT_CLAMP, ROT_INPUT_CLAMP);
        float rotateYForTick = Mth.clamp(this.emaRotateY * this.rotateSensitivity * inputSensScale, -ROT_INPUT_CLAMP, ROT_INPUT_CLAMP);
        float scrollForTick = this.pendingScroll * inputSensScale;

        if (forward != 0.0F || strafe != 0.0F
                || this.pendingPanX != 0.0F || this.pendingPanY != 0.0F
                || rotateXForTick != 0.0F || rotateYForTick != 0.0F
                || scrollForTick != 0.0F || this.pendingRotateSteps != 0) {
            this.applyLocalPrediction(
                    forward,
                    strafe,
                    this.pendingPanX,
                    this.pendingPanY,
                    rotateXForTick,
                    rotateYForTick,
                    scrollForTick,
                    this.pendingRotateSteps,
                    shift);
        }

        // Fixed packet frequency: one packet per client tick while RTS is enabled.
        PacketDistributor.sendToServer(new C2SRtsCameraMovePayload(
                forward,
                strafe,
                this.pendingPanX,
                this.pendingPanY,
                rotateXForTick,
                rotateYForTick,
                scrollForTick,
                this.pendingRotateSteps,
                shift));

        this.pendingPanX = 0.0F;
        this.pendingPanY = 0.0F;
        this.pendingScroll = 0.0F;
        this.pendingRotateSteps = 0;
        this.pendingRawRotateX = 0.0F;
        this.pendingRawRotateY = 0.0F;

        this.syncVisualCameraFrame();
    }

    public void queuePanDrag(double dragX, double dragY) {
        this.pendingPanX += (float) dragX;
        this.pendingPanY += (float) dragY;
    }

    public void queueRotateDrag(double dragX, double dragY) {
        this.pendingRawRotateX += (float) dragX;
        this.pendingRawRotateY += (float) dragY;
    }

    public void queueScroll(double scrollY) {
        this.pendingScroll += (float) scrollY;
    }

    public void queueRotateQuarter(int direction) {
        this.pendingRotateSteps += direction;
    }

    public void updateFunnelTarget(BlockPos target) {
        if (!this.funnelEnabled || target == null) {
            return;
        }
        if (this.funnelTargetCooldownTicks > 0) {
            return;
        }
        if (this.lastFunnelTarget != null && this.lastFunnelTarget.equals(target)) {
            return;
        }
        this.lastFunnelTarget = target.immutable();
        this.funnelTargetCooldownTicks = 2;
        PacketDistributor.sendToServer(new C2SRtsFunnelTargetPayload(this.lastFunnelTarget));
    }

    public void linkStorage(BlockPos pos) {
        linkStorage(pos, true);
    }

    public void linkStorage(BlockPos pos, boolean allowStore) {
        if (pos == null) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsLinkStoragePayload(
                pos,
                allowStore ? C2SRtsLinkStoragePayload.MODE_BIDIRECTIONAL : C2SRtsLinkStoragePayload.MODE_EXTRACT_ONLY));
    }

    public void requestStoragePage(int page) {
        PacketDistributor.sendToServer(new C2SRtsRequestStoragePagePayload(
                page,
                this.storageSearch,
                this.storageCategory,
                (byte) this.storageSort.ordinal(),
                this.storageSortAscending));
    }

    public void requestCraftables() {
        this.craftablesSearch = normalizeCraftablesSearch(this.craftablesSearch);
        clearCraftablesState();
        if (this.craftablesSearch.isBlank()) {
            return;
        }
        requestCraftablesPage(0, CRAFTABLE_BATCH_SIZE);
    }

    public void requestMoreCraftables() {
        if (this.craftablesSearch.isBlank() || !this.craftablesHasMore) {
            return;
        }
        requestCraftablesPage(this.craftableEntries.size(), CRAFTABLE_BATCH_SIZE);
    }

    public void setAutoStoreMinedDrops(boolean enabled) {
        this.autoStoreMinedDrops = enabled;
        PacketDistributor.sendToServer(new C2SRtsSetAutoStorePayload(enabled));
    }

    public void toggleAutoStoreMinedDrops() {
        setAutoStoreMinedDrops(!this.autoStoreMinedDrops);
    }

    public void setStorageSearch(String search) {
        this.storageSearch = search == null ? "" : search;
        requestStoragePage(0);
    }

    public void setStorageCategory(String category) {
        String normalized = normalizeCategory(category);
        if (this.storageCategory.equals(normalized)) {
            return;
        }
        this.storageCategory = normalized;
        requestStoragePage(0);
    }

    public void cycleSort() {
        int next = (this.storageSort.ordinal() + 1) % RtsStorageSort.values().length;
        this.storageSort = RtsStorageSort.byId(next);
        requestStoragePage(0);
    }

    public void toggleSortDirection() {
        this.storageSortAscending = !this.storageSortAscending;
        requestStoragePage(0);
    }

    public void prevPage() {
        requestStoragePage(Math.max(0, this.storagePage - 1));
    }

    public void nextPage() {
        requestStoragePage(Math.min(this.storageTotalPages - 1, this.storagePage + 1));
    }

    public void setCraftablesSearch(String search) {
        String normalized = normalizeCraftablesSearch(search);
        if (this.craftablesSearch.equals(normalized)) {
            return;
        }
        this.craftablesSearch = normalized;
        requestCraftables();
    }

    public void setCraftablesShowUnavailable(boolean showUnavailable) {
        if (this.craftablesShowUnavailable == showUnavailable) {
            return;
        }
        this.craftablesShowUnavailable = showUnavailable;
        requestCraftables();
    }

    public void toggleCraftablesShowUnavailable() {
        setCraftablesShowUnavailable(!this.craftablesShowUnavailable);
    }

    public void craftRecipeToLinked(String recipeId) {
        craftRecipeToLinked(recipeId, 1);
    }

    public void craftRecipeToLinked(String recipeId, int craftCount) {
        if (recipeId == null || recipeId.isBlank()) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsCraftRecipePayload(recipeId, Math.max(1, craftCount)));
    }

    public void openCraftTerminal() {
        setStorageSearch("");
        this.pendingCraftTerminalOpen = true;
        this.pendingCraftTerminalOpenTicks = 120;
        beginRemoteMenuOpenGrace(null);
        PacketDistributor.sendToServer(new C2SRtsOpenCraftTerminalPayload());
    }

    public void detectQuestsNow() {
        PacketDistributor.sendToServer(new C2SRtsQuestDetectPayload(C2SRtsQuestDetectPayload.MODE_MANUAL));
    }

    public void rotateBlock(BlockPos pos) {
        if (pos == null) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsRotateBlockPayload(pos));
    }

    public void storeHotbarSlotToLinked(int slot) {
        PacketDistributor.sendToServer(new C2SRtsStoreHotbarSlotPayload((byte) Mth.clamp(slot, 0, 8)));
    }

    public void fillInventoryFromLinked() {
        PacketDistributor.sendToServer(new C2SRtsFillInventoryPayload());
    }

    private boolean shouldUseRtsCraftTerminalScreen(CraftingScreen craftingScreen) {
        if (this.pendingCraftTerminalOpen) {
            return true;
        }
        return craftingScreen.getTitle() != null
                && "RTS Craft Terminal".equals(craftingScreen.getTitle().getString());
    }

    public void quickDropSelectedItem(String itemId, int amount, Vec3 dropPos) {
        if (itemId == null || itemId.isBlank() || dropPos == null) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsQuickDropPayload(
                itemId,
                (byte) Mth.clamp(amount, 1, 64),
                dropPos.x,
                dropPos.y,
                dropPos.z));
    }

    public void applyStoragePage(S2CRtsStoragePagePayload payload) {
        this.storageLinked = payload.linked();
        this.linkedStorageName = payload.linkedName();
        this.autoStoreMinedDrops = payload.autoStoreMinedDrops();
        this.linkedStoragePositions.clear();
        for (Long packed : payload.linkedPositions()) {
            if (packed == null) {
                continue;
            }
            this.linkedStoragePositions.add(BlockPos.of(packed.longValue()));
        }
        this.storagePage = payload.page();
        this.storageTotalPages = Math.max(1, payload.totalPages());
        this.storageTotalEntries = payload.totalEntries();
        this.storageSearch = payload.search();
        this.storageCategory = normalizeCategory(payload.category());
        this.storageSort = RtsStorageSort.byId(payload.sort());
        this.storageSortAscending = payload.ascending();
        this.storageCategories.clear();
        this.storageCategories.add("all");
        for (String category : payload.categories()) {
            String normalized = normalizeCategory(category);
            if (!this.storageCategories.contains(normalized)) {
                this.storageCategories.add(normalized);
            }
        }
        if (!this.storageCategories.contains(this.storageCategory)) {
            this.storageCategory = "all";
        }
        this.storageEntries.clear();
        this.storageTotalCounts.clear();
        this.fluidEntries.clear();
        this.recentEntries.clear();

        int size = Math.min(payload.itemIds().size(), payload.counts().size());
        for (int i = 0; i < size; i++) {
            ResourceLocation id = ResourceLocation.tryParse(payload.itemIds().get(i));
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            this.storageEntries.add(new StorageEntry(stack, payload.itemIds().get(i), payload.counts().get(i), id.getNamespace(), id.getPath()));
        }

        int totalItemSize = Math.min(payload.totalItemIds().size(), payload.totalItemCounts().size());
        for (int i = 0; i < totalItemSize; i++) {
            String itemId = payload.totalItemIds().get(i);
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            this.storageTotalCounts.put(itemId, Math.max(0L, payload.totalItemCounts().get(i)));
        }

        int fluidSize = Math.min(payload.fluidIds().size(),
                Math.min(payload.fluidAmounts().size(), payload.fluidCapacities().size()));
        for (int i = 0; i < fluidSize; i++) {
            String fluidId = payload.fluidIds().get(i);
            ResourceLocation id = ResourceLocation.tryParse(fluidId);
            if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) {
                continue;
            }
            Fluid fluid = BuiltInRegistries.FLUID.get(id);
            FluidStack fluidStack = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
            ItemStack preview = FluidUtil.getFilledBucket(fluidStack);
            String label = fluid.getFluidType().getDescription(fluidStack).getString();
            this.fluidEntries.add(new FluidEntry(
                    fluidId,
                    label,
                    payload.fluidAmounts().get(i),
                    payload.fluidCapacities().get(i),
                    id.getNamespace(),
                    id.getPath(),
                    preview));
        }

        int recentSize = Math.min(
                payload.recentIds().size(),
                Math.min(
                        payload.recentAmounts().size(),
                        Math.min(payload.recentCapacities().size(), payload.recentKinds().size())));
        for (int i = 0; i < recentSize; i++) {
            RecentEntry entry = decodeRecentEntry(
                    payload.recentIds().get(i),
                    payload.recentAmounts().get(i),
                    payload.recentCapacities().get(i),
                    payload.recentKinds().get(i));
            if (entry != null) {
                this.recentEntries.add(entry);
            }
        }

        applyQuickSlotPayload(payload.quickSlotItemIds());
        applyGuiBindingPayload(payload.guiBindingLabels());

        this.funnelEnabled = payload.funnelEnabled();
        this.funnelBufferEntries.clear();
        int funnelBufferSize = Math.min(payload.funnelBufferItemIds().size(), payload.funnelBufferCounts().size());
        for (int i = 0; i < funnelBufferSize; i++) {
            String itemId = payload.funnelBufferItemIds().get(i);
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            long count = Math.max(0L, payload.funnelBufferCounts().get(i));
            if (count <= 0L) {
                continue;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            this.funnelBufferEntries.add(new FunnelBufferEntry(stack, itemId, count));
        }
        this.storageRevision++;
        if (!this.storageLinked && this.linkedStoragePositions.isEmpty()) {
            clearCraftablesState();
        }
    }

    public void applyRemoteMenuHint(S2CRtsRemoteMenuHintPayload payload) {
        if (payload == null || payload.pos() == null) {
            return;
        }
        BlockPos pos = payload.pos().immutable();
        this.pendingRemoteMenuOpenTicks = Math.max(this.pendingRemoteMenuOpenTicks, REMOTE_MENU_OPEN_GRACE_TICKS);
        this.pendingRemoteMenuValidationPos = pos;
        this.activeRemoteMenuValidationPos = pos;
    }

    public void applyCraftables(S2CRtsCraftablesPayload payload) {
        String payloadSearch = normalizeCraftablesSearch(payload.search());
        if (!this.craftablesSearch.equals(payloadSearch)
                || this.craftablesShowUnavailable != payload.showUnavailable()) {
            return;
        }

        int offset = Math.max(0, payload.offset());
        this.pendingCraftableOffsets.remove(offset);
        if (!payload.append() || offset == 0) {
            this.craftableEntries.clear();
        } else if (offset != this.craftableEntries.size()) {
            return;
        }

        int size = Math.min(
                payload.recipeIds().size(),
                Math.min(
                        payload.resultItemIds().size(),
                        Math.min(
                                payload.resultCounts().size(),
                                Math.min(payload.craftable().size(), payload.missingSummaries().size()))));
        int optionFlatIndex = 0;
        for (int i = 0; i < size; i++) {
            ResourceLocation id = ResourceLocation.tryParse(payload.resultItemIds().get(i));
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                optionFlatIndex += i < payload.recipeOptionCounts().size() ? Math.max(0, payload.recipeOptionCounts().get(i)) : 0;
                continue;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            int resultCount = Math.max(1, payload.resultCounts().get(i));
            stack.setCount(Math.min(resultCount, stack.getMaxStackSize()));
            int optionCount = i < payload.recipeOptionCounts().size() ? Math.max(0, payload.recipeOptionCounts().get(i)) : 0;
            List<CraftRecipeOption> recipeOptions = new ArrayList<>(optionCount);
            for (int optionIndex = 0; optionIndex < optionCount; optionIndex++) {
                if (optionFlatIndex >= payload.optionRecipeIds().size()
                        || optionFlatIndex >= payload.optionResultCounts().size()
                        || optionFlatIndex >= payload.optionCraftable().size()
                        || optionFlatIndex >= payload.optionSummaries().size()
                        || optionFlatIndex >= payload.optionMissingSummaries().size()) {
                    break;
                }
                recipeOptions.add(new CraftRecipeOption(
                        payload.optionRecipeIds().get(optionFlatIndex),
                        Math.max(1, payload.optionResultCounts().get(optionFlatIndex)),
                        payload.optionCraftable().get(optionFlatIndex),
                        payload.optionSummaries().get(optionFlatIndex),
                        payload.optionMissingSummaries().get(optionFlatIndex)));
                optionFlatIndex++;
            }
            if (recipeOptions.isEmpty()) {
                recipeOptions.add(new CraftRecipeOption(
                        payload.recipeIds().get(i),
                        resultCount,
                        payload.craftable().get(i),
                        stack.getHoverName().getString(),
                        payload.missingSummaries().get(i)));
            }
            this.craftableEntries.add(new CraftableEntry(
                    stack,
                    payload.recipeIds().get(i),
                    payload.resultItemIds().get(i),
                    resultCount,
                    payload.craftable().get(i),
                    payload.missingSummaries().get(i),
                    id.getNamespace(),
                    id.getPath(),
                    List.copyOf(recipeOptions)));
        }
        this.craftablesSearch = payloadSearch;
        this.craftablesShowUnavailable = payload.showUnavailable();
        this.craftablesHasMore = payload.hasMore();
        this.craftablesRevision++;
    }

    private void requestCraftablesPage(int offset, int limit) {
        int normalizedOffset = Math.max(0, offset);
        int normalizedLimit = Math.max(1, limit);
        if (!this.pendingCraftableOffsets.add(normalizedOffset)) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsRequestCraftablesPayload(
                this.craftablesSearch,
                this.craftablesShowUnavailable,
                normalizedOffset,
                normalizedLimit));
    }

    private void clearCraftablesState() {
        boolean changed = !this.craftableEntries.isEmpty()
                || this.craftablesHasMore
                || !this.pendingCraftableOffsets.isEmpty();
        this.craftableEntries.clear();
        this.craftablesHasMore = false;
        this.pendingCraftableOffsets.clear();
        if (changed) {
            this.craftablesRevision++;
        }
    }

    private static String normalizeCraftablesSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private void applyStoredLayout(RtsClientLayoutStore.StoragePanelLayout layout) {
        RtsClientLayoutStore.StoragePanelLayout safe = layout == null
                ? RtsClientLayoutStore.loadStoragePanelLayout()
                : layout;
        this.storagePanelXNormalized = clampLayoutNormalized(safe.xNormalized());
        this.storagePanelYNormalized = clampLayoutNormalized(safe.yNormalized());
        this.storagePanelWidthNormalized = clampLayoutNormalized(safe.widthNormalized());
        this.storagePanelHeightNormalized = clampLayoutNormalized(safe.heightNormalized());
    }

    private static double clampLayoutNormalized(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Mth.clamp(value, 0.0D, 1.0D);
    }

    private long getStorageFluidAmount(String fluidId) {
        if (fluidId == null || fluidId.isBlank()) {
            return 0L;
        }
        for (FluidEntry entry : this.fluidEntries) {
            if (fluidId.equals(entry.fluidId())) {
                return Math.max(0L, entry.amount());
            }
        }
        return 0L;
    }

    public void applyCraftFeedback(S2CRtsCraftFeedbackPayload payload) {
        String itemId = payload.itemId() == null ? "" : payload.itemId();
        int craftedCount = Math.max(0, payload.craftedCount());
        if (itemId.isBlank() || craftedCount <= 0) {
            return;
        }
        List<CraftFeedbackIngredient> decodedIngredients = new ArrayList<>();
        int ingredientSize = Math.min(payload.consumedItemIds().size(), payload.consumedCounts().size());
        for (int i = 0; i < ingredientSize; i++) {
            String consumedItemId = payload.consumedItemIds().get(i);
            ResourceLocation consumedKey = ResourceLocation.tryParse(consumedItemId);
            if (consumedKey == null || !BuiltInRegistries.ITEM.containsKey(consumedKey)) {
                continue;
            }
            ItemStack preview = new ItemStack(BuiltInRegistries.ITEM.get(consumedKey));
            decodedIngredients.add(new CraftFeedbackIngredient(
                    consumedItemId,
                    preview.getHoverName().getString(),
                    preview,
                    Math.max(0, payload.consumedCounts().get(i))));
        }
        long now = System.currentTimeMillis();
        boolean mergeWithActive = itemId.equals(this.craftFeedbackItemId) && now <= this.craftFeedbackExpiryMs;
        if (mergeWithActive) {
            this.craftFeedbackCount += craftedCount;
        } else {
            this.craftFeedbackItemId = itemId;
            this.craftFeedbackCount = craftedCount;
        }
        if (mergeWithActive) {
            mergeCraftFeedbackIngredients(decodedIngredients);
        } else {
            this.craftFeedbackIngredients.clear();
            this.craftFeedbackIngredients.addAll(decodedIngredients);
        }
        this.craftFeedbackExpiryMs = now + 2200L;
    }

    private void mergeCraftFeedbackIngredients(List<CraftFeedbackIngredient> added) {
        if (added == null || added.isEmpty()) {
            return;
        }
        Map<String, CraftFeedbackIngredient> merged = new LinkedHashMap<>();
        for (CraftFeedbackIngredient ingredient : this.craftFeedbackIngredients) {
            if (ingredient == null || ingredient.itemId() == null || ingredient.itemId().isBlank()) {
                continue;
            }
            merged.put(ingredient.itemId(), ingredient);
        }
        for (CraftFeedbackIngredient ingredient : added) {
            if (ingredient == null || ingredient.itemId() == null || ingredient.itemId().isBlank()) {
                continue;
            }
            CraftFeedbackIngredient existing = merged.get(ingredient.itemId());
            if (existing == null) {
                merged.put(ingredient.itemId(), ingredient);
                continue;
            }
            merged.put(
                    ingredient.itemId(),
                    new CraftFeedbackIngredient(
                            ingredient.itemId(),
                            ingredient.label(),
                            ingredient.preview().copy(),
                            existing.count() + ingredient.count()));
        }
        this.craftFeedbackIngredients.clear();
        this.craftFeedbackIngredients.addAll(merged.values());
    }

    public void applyMineProgress(S2CRtsMineProgressPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        BlockPos pos = payload.pos();
        int stage = payload.stage();
        if (stage < 0) {
            if (this.mineRenderPos != null) {
                minecraft.level.destroyBlockProgress(RTS_MINE_RENDER_ID, this.mineRenderPos, -1);
                this.mineRenderPos = null;
            } else {
                minecraft.level.destroyBlockProgress(RTS_MINE_RENDER_ID, pos, -1);
            }
            return;
        }

        if (this.mineRenderPos != null && !this.mineRenderPos.equals(pos)) {
            minecraft.level.destroyBlockProgress(RTS_MINE_RENDER_ID, this.mineRenderPos, -1);
        }
        minecraft.level.destroyBlockProgress(RTS_MINE_RENDER_ID, pos, Math.min(9, stage));
        this.mineRenderPos = pos.immutable();
    }

    public void selectStorageEntry(int index) {
        if (index < 0 || index >= this.storageEntries.size()) {
            return;
        }
        StorageEntry entry = this.storageEntries.get(index);
        setSelectedItem(entry.itemId(), entry.stack().getHoverName().getString(), entry.stack().copy());
        clearSelectedFluid();
        setMode(BuilderMode.INTERACT);
    }

    public void selectFluidEntry(int index) {
        if (index < 0 || index >= this.fluidEntries.size()) {
            return;
        }
        FluidEntry entry = this.fluidEntries.get(index);
        setSelectedFluid(entry.fluidId(), entry.label(), entry.preview().copy());
        clearSelectedItemOnly();
        setMode(BuilderMode.INTERACT);
    }

    public void clearSelectedItem() {
        clearPlacementSelectionPreserveMode();
        setMode(BuilderMode.INTERACT);
    }

    public void clearPlacementSelectionPreserveMode() {
        clearSelectedItemOnly();
        clearSelectedFluid();
        this.placeRotateSteps = 0;
    }

    public void selectRecentEntry(int index) {
        if (index < 0 || index >= this.recentEntries.size()) {
            return;
        }
        RecentEntry entry = this.recentEntries.get(index);
        if (entry.fluid()) {
            setSelectedFluid(entry.id(), entry.label(), entry.preview().copy());
            clearSelectedItemOnly();
        } else {
            setSelectedItem(entry.id(), entry.label(), entry.preview().copy());
            clearSelectedFluid();
        }
        setMode(BuilderMode.INTERACT);
    }

    public void assignQuickSlotFromSelected(int index) {
        if (index < 0 || index >= QUICK_SLOT_COUNT) {
            return;
        }
        if (this.selectedItemId.isBlank() || this.selectedItemPreview.isEmpty()) {
            clearQuickSlot(index);
            return;
        }
        setQuickSlotLocal(index, this.selectedItemId, this.selectedItemPreview.copy());
        PacketDistributor.sendToServer(new C2SRtsSetQuickSlotPayload((byte) index, this.selectedItemId));
    }

    public void assignQuickSlotFromToolItem(int index, ItemStack stack) {
        if (index < 0 || index >= QUICK_SLOT_COUNT || stack == null || stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        String itemId = id.toString();
        setQuickSlotLocal(index, itemId, stack.copy());
        PacketDistributor.sendToServer(new C2SRtsSetQuickSlotPayload((byte) index, itemId));
    }

    public void clearQuickSlot(int index) {
        if (index < 0 || index >= QUICK_SLOT_COUNT) {
            return;
        }
        setQuickSlotLocal(index, "", ItemStack.EMPTY);
        PacketDistributor.sendToServer(new C2SRtsSetQuickSlotPayload((byte) index, ""));
    }

    public void selectQuickSlot(int index) {
        if (index < 0 || index >= QUICK_SLOT_COUNT) {
            return;
        }
        String itemId = this.quickSlotItemIds[index];
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        ItemStack preview = this.quickSlotPreviews[index];
        if (preview.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                return;
            }
            preview = new ItemStack(BuiltInRegistries.ITEM.get(id));
        }
        String label = this.quickSlotLabels[index];
        if (label == null || label.isBlank()) {
            label = preview.getHoverName().getString();
        }
        setSelectedItem(itemId, label, preview.copy());
        clearSelectedFluid();
        setMode(BuilderMode.INTERACT);
    }

    public void setGuiBinding(int index, BlockPos pos) {
        if (index < 0 || index >= GUI_BINDING_SLOT_COUNT || pos == null) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsSetGuiBindingPayload((byte) index, false, pos));
    }

    public void clearGuiBinding(int index) {
        if (index < 0 || index >= GUI_BINDING_SLOT_COUNT) {
            return;
        }
        this.guiBindingLabels[index] = "";
        PacketDistributor.sendToServer(new C2SRtsSetGuiBindingPayload((byte) index, true, BlockPos.ZERO));
    }

    public void openGuiBinding(int index) {
        if (index < 0 || index >= GUI_BINDING_SLOT_COUNT || !hasGuiBinding(index)) {
            return;
        }
        beginRemoteMenuOpenGrace(null);
        PacketDistributor.sendToServer(new C2SRtsOpenGuiBindingPayload((byte) index));
    }

    public void placeSelected(BlockHitResult hit, boolean forcePlace, Vec3 rayOrigin, Vec3 rayDir) {
        placeSelected(hit, forcePlace, rayOrigin, rayDir, false);
    }

    public void placeSelected(BlockHitResult hit, boolean forcePlace, Vec3 rayOrigin, Vec3 rayDir, boolean skipIfOccupied) {
        beginRemoteMenuOpenGrace(hit == null ? null : hit.getBlockPos());
        PacketDistributor.sendToServer(new C2SRtsPlacePayload(
                hit.getBlockPos(),
                (byte) hit.getDirection().get3DDataValue(),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                (byte) (this.selectedItemId.isBlank() ? 0 : this.placeRotateSteps),
                forcePlace,
                skipIfOccupied,
                this.selectedItemId,
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    public void placeSelectedFluid(BlockHitResult hit, boolean forcePlace, Vec3 rayOrigin, Vec3 rayDir) {
        if (hit == null || this.selectedFluidId.isBlank()) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsPlaceFluidPayload(
                hit.getBlockPos(),
                (byte) hit.getDirection().get3DDataValue(),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                forcePlace,
                this.selectedFluidId,
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    public void storeFluidFromStorageItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsStoreFluidPayload(
                C2SRtsStoreFluidPayload.SOURCE_STORAGE_ITEM,
                (byte) 0,
                itemId));
    }

    public void storeFluidFromPinnedItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsStoreFluidPayload(
                C2SRtsStoreFluidPayload.SOURCE_PIN_ITEM,
                (byte) 0,
                itemId));
    }

    public void storeFluidFromToolSlot(int toolSlot) {
        PacketDistributor.sendToServer(new C2SRtsStoreFluidPayload(
                C2SRtsStoreFluidPayload.SOURCE_TOOL_SLOT,
                (byte) Mth.clamp(toolSlot, 0, 8),
                ""));
    }

    private void clearQuickSlotsLocal() {
        for (int i = 0; i < QUICK_SLOT_COUNT; i++) {
            this.quickSlotItemIds[i] = "";
            this.quickSlotLabels[i] = "";
            this.quickSlotPreviews[i] = ItemStack.EMPTY;
        }
    }

    private void clearGuiBindingsLocal() {
        for (int i = 0; i < GUI_BINDING_SLOT_COUNT; i++) {
            this.guiBindingLabels[i] = "";
        }
    }

    private void applyQuickSlotPayload(List<String> payloadQuickSlots) {
        clearQuickSlotsLocal();
        int size = Math.min(QUICK_SLOT_COUNT, payloadQuickSlots == null ? 0 : payloadQuickSlots.size());
        for (int i = 0; i < size; i++) {
            String itemId = payloadQuickSlots.get(i);
            if (itemId == null || itemId.isBlank()) {
                continue;
            }
            ResourceLocation key = ResourceLocation.tryParse(itemId);
            if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
                continue;
            }
            ItemStack preview = new ItemStack(BuiltInRegistries.ITEM.get(key));
            setQuickSlotLocal(i, itemId, preview);
        }
    }

    private void applyGuiBindingPayload(List<String> payloadGuiBindings) {
        clearGuiBindingsLocal();
        int size = Math.min(GUI_BINDING_SLOT_COUNT, payloadGuiBindings == null ? 0 : payloadGuiBindings.size());
        for (int i = 0; i < size; i++) {
            String label = payloadGuiBindings.get(i);
            this.guiBindingLabels[i] = label == null ? "" : label;
        }
    }

    private void setQuickSlotLocal(int index, String itemId, ItemStack preview) {
        if (index < 0 || index >= QUICK_SLOT_COUNT) {
            return;
        }
        String normalizedItemId = itemId == null ? "" : itemId;
        ItemStack normalizedPreview = preview == null ? ItemStack.EMPTY : preview.copy();
        this.quickSlotItemIds[index] = normalizedItemId;
        if (normalizedItemId.isBlank() || normalizedPreview.isEmpty()) {
            this.quickSlotLabels[index] = "";
            this.quickSlotPreviews[index] = ItemStack.EMPTY;
            return;
        }
        this.quickSlotLabels[index] = normalizedPreview.getHoverName().getString();
        this.quickSlotPreviews[index] = normalizedPreview;
    }

    public void interactEmpty(BlockHitResult hit, Vec3 rayOrigin, Vec3 rayDir) {
        beginRemoteMenuOpenGrace(hit == null ? null : hit.getBlockPos());
        PacketDistributor.sendToServer(new C2SRtsPlacePayload(
                hit.getBlockPos(),
                (byte) hit.getDirection().get3DDataValue(),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                (byte) 0,
                false,
                false,
                "",
                rayOrigin.x,
                rayOrigin.y,
                rayOrigin.z,
                rayDir.x,
                rayDir.y,
                rayDir.z));
    }

    public void interactBlockWithToolSlot(BlockHitResult hit, int toolSlot, Vec3 rayOrigin, Vec3 rayDir) {
        if (hit == null) {
            return;
        }
        beginRemoteMenuOpenGrace(hit.getBlockPos());
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

    public void interactBlockWithPinnedItem(BlockHitResult hit, String itemId, Vec3 rayOrigin, Vec3 rayDir) {
        if (hit == null || itemId == null || itemId.isBlank()) {
            return;
        }
        beginRemoteMenuOpenGrace(hit.getBlockPos());
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

    public void interactEntityWithToolSlot(int entityId, Vec3 hitLocation, int toolSlot, Vec3 rayOrigin, Vec3 rayDir) {
        if (entityId < 0 || hitLocation == null) {
            return;
        }
        beginRemoteMenuOpenGrace(BlockPos.containing(hitLocation));
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

    public void interactEntityWithPinnedItem(int entityId, Vec3 hitLocation, String itemId, Vec3 rayOrigin, Vec3 rayDir) {
        if (entityId < 0 || hitLocation == null || itemId == null || itemId.isBlank()) {
            return;
        }
        beginRemoteMenuOpenGrace(BlockPos.containing(hitLocation));
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

    public void breakPlaced(BlockPos pos) {
        breakPlaced(pos, Direction.UP, false);
    }

    public void breakPlaced(BlockPos pos, Direction face, boolean allowAdjacentFallback) {
        if (pos == null) {
            return;
        }
        Direction resolvedFace = face == null ? Direction.UP : face;
        PacketDistributor.sendToServer(new C2SRtsBreakPayload(
                pos,
                (byte) resolvedFace.get3DDataValue(),
                allowAdjacentFallback));
    }

    public void startMining(BlockPos pos, int face, int toolSlot) {
        if (pos == null) {
            return;
        }
        this.activeMinePos = pos.immutable();
        this.activeMineFace = face;
        this.activeMineToolSlot = Mth.clamp(toolSlot, 0, 8);
        PacketDistributor.sendToServer(new C2SRtsMinePayload(this.activeMinePos, (byte) face, true, (byte) this.activeMineToolSlot));
    }

    public void continueMining(int toolSlot) {
        // Mining progress is maintained server-side after START; no per-tick packet needed.
    }

    public void abortMining(int toolSlot) {
        if (this.activeMinePos == null || this.activeMineFace < 0) {
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsMinePayload(
                this.activeMinePos,
                (byte) this.activeMineFace,
                false,
                (byte) toolSlot));
        this.activeMinePos = null;
        this.activeMineFace = -1;
    }

    private void beginRemoteMenuOpenGrace(BlockPos menuPos) {
        this.pendingRemoteMenuOpenTicks = Math.max(this.pendingRemoteMenuOpenTicks, REMOTE_MENU_OPEN_GRACE_TICKS);
        this.pendingRemoteMenuValidationPos = menuPos == null ? null : menuPos.immutable();
        if (menuPos == null) {
            this.activeRemoteMenuValidationPos = null;
        }
    }

    private void restoreRemoteMenuValidationPosition(Minecraft minecraft) {
        if (!this.remoteMenuValidationSpoofed || minecraft == null || minecraft.player == null || this.remoteMenuRestorePos == null) {
            this.remoteMenuValidationSpoofed = false;
            this.remoteMenuRestorePos = null;
            return;
        }
        minecraft.player.setPos(this.remoteMenuRestorePos.x, this.remoteMenuRestorePos.y, this.remoteMenuRestorePos.z);
        this.remoteMenuValidationSpoofed = false;
        this.remoteMenuRestorePos = null;
    }

    private void clearRemoteMenuValidationState() {
        this.pendingRemoteMenuValidationPos = null;
        this.activeRemoteMenuValidationPos = null;
        this.remoteMenuRestorePos = null;
        this.remoteMenuValidationSpoofed = false;
        this.relaxedRemoteMenu = null;
    }

    private static Vec3 resolveMenuValidationPosition(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D);
    }

    private static void relaxClientMenuValidation(AbstractContainerMenu menu) {
        if (menu == null) {
            return;
        }
        Class<?> type = menu.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();

                    if (ContainerLevelAccess.class.isAssignableFrom(fieldType)) {
                        Object current = field.get(menu);
                        if (current instanceof ContainerLevelAccess access
                                && !(access instanceof ClientRelaxedContainerLevelAccess)) {
                            field.set(menu, new ClientRelaxedContainerLevelAccess(access));
                        } else if (current == null) {
                            field.set(menu, ContainerLevelAccess.NULL);
                        }
                        continue;
                    }

                    if (fieldType == Container.class) {
                        Object current = field.get(menu);
                        if (current instanceof Container delegate && !(delegate instanceof ClientAlwaysValidContainer)) {
                            field.set(menu, new ClientAlwaysValidContainer(delegate));
                        }
                    }
                } catch (ReflectiveOperationException ignored) {
                    // Some runtime-specific/final fields cannot be patched reflectively.
                }
            }
            type = type.getSuperclass();
        }
    }

    private void clearSelectedItemOnly() {
        setSelectedItem("", "", ItemStack.EMPTY);
    }

    private void clearSelectedFluid() {
        setSelectedFluid("", "", ItemStack.EMPTY);
    }

    private void setSelectedItem(String itemId, String label, ItemStack preview) {
        this.selectedItemId = itemId == null ? "" : itemId;
        this.selectedItemLabel = label == null ? "" : label;
        this.selectedItemPreview = preview == null ? ItemStack.EMPTY : preview;
    }

    private void setSelectedFluid(String fluidId, String label, ItemStack preview) {
        this.selectedFluidId = fluidId == null ? "" : fluidId;
        this.selectedFluidLabel = label == null ? "" : label;
        this.selectedFluidPreview = preview == null ? ItemStack.EMPTY : preview;
    }

    public void rotatePlacementClockwise() {
        this.placeRotateSteps = (this.placeRotateSteps + 1) & 3;
    }

    public void rotatePlacementCounterClockwise() {
        this.placeRotateSteps = (this.placeRotateSteps + 3) & 3;
    }

    public void syncVisualCameraFrame() {
        if (!this.enabled || !this.localStateReady) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        this.ensureLocalMirrorCamera(minecraft);
        if (this.localMirrorCamera == null) {
            return;
        }

        this.localMirrorCamera.snapTo(this.localX, this.localY, this.localZ, this.localYawDeg, this.localPitchDeg);

        if (minecraft.getCameraEntity() != this.localMirrorCamera) {
            minecraft.setCameraEntity(this.localMirrorCamera);
        }
    }

    private void ensureLocalMirrorCamera(Minecraft minecraft) {
        if (minecraft.level == null) {
            this.localMirrorCamera = null;
            return;
        }

        if (this.localMirrorCamera != null && this.localMirrorCamera.level() == minecraft.level) {
            return;
        }

        this.localMirrorCamera = new RtsCameraEntity(RtsbuildingMod.RTS_CAMERA_ENTITY.get(), minecraft.level);
        this.localMirrorCamera.snapTo(this.localX, this.localY, this.localZ, this.localYawDeg, this.localPitchDeg);
    }

    private void applyLocalPrediction(float forward, float strafe, float panX, float panY, float rotateX, float rotateY,
            float scroll, int rotateSteps, boolean fast) {
        this.localYawDeg += rotateX * ROTATE_GAIN_X;
        if (rotateSteps != 0) {
            this.localYawDeg = snapQuarter(this.localYawDeg + (90.0F * rotateSteps));
        }
        this.localPitchDeg = Mth.clamp(this.localPitchDeg + (rotateY * ROTATE_GAIN_Y), MIN_CAMERA_PITCH, MAX_CAMERA_PITCH);

        double speed = fast ? 0.80D : 0.45D;
        double yawRad = Math.toRadians(this.localYawDeg);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double targetX = this.localX;
        double targetY = this.localY;
        double targetZ = this.localZ;

        double dx = (-sin * forward + cos * strafe) * speed;
        double dz = (cos * forward + sin * strafe) * speed;

        double dragScale = 0.020D * Math.max(8.0D, this.localHeightOffset);
        double moveRight = panX * dragScale;
        double moveForward = -panY * dragScale;

        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);

        dx += rightX * moveRight + fwdX * moveForward;
        dz += rightZ * moveRight + fwdZ * moveForward;

        targetX += dx;
        targetZ += dz;

        if (scroll != 0.0F) {
            double pitchRad = Math.toRadians(this.localPitchDeg);
            double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
            double lookY = -Math.sin(pitchRad);
            double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

            double dolly = scroll * DOLLY_PER_SCROLL;
            targetX += lookX * dolly;
            targetY += lookY * dolly;
            targetZ += lookZ * dolly;
        }

        double adx = targetX - this.anchorX;
        double adz = targetZ - this.anchorZ;
        double distSqr = adx * adx + adz * adz;
        double maxSqr = this.maxRadius * this.maxRadius;
        if (distSqr > maxSqr) {
            double dist = Math.sqrt(distSqr);
            double scale = this.maxRadius / dist;
            targetX = this.anchorX + (adx * scale);
            targetZ = this.anchorZ + (adz * scale);
        }

        targetY = Mth.clamp(targetY, this.anchorY + MIN_CAMERA_HEIGHT_OFFSET, this.anchorY + MAX_CAMERA_HEIGHT_OFFSET);

        Vec3 toCam = new Vec3(targetX - this.anchorX, targetY - this.anchorY, targetZ - this.anchorZ);
        double dist = toCam.length();
        if (dist > 1.0e-6) {
            double clamped = Mth.clamp(dist, 8.0D, 72.0D);
            if (Math.abs(clamped - dist) > 1.0e-4) {
                Vec3 n = toCam.scale(clamped / dist);
                targetX = this.anchorX + n.x;
                targetY = this.anchorY + n.y;
                targetZ = this.anchorZ + n.z;
            }
        }

        targetY = Mth.clamp(targetY, this.anchorY + MIN_CAMERA_HEIGHT_OFFSET, this.anchorY + MAX_CAMERA_HEIGHT_OFFSET);

        this.localX = targetX;
        this.localY = targetY;
        this.localZ = targetZ;
        this.localHeightOffset = this.localY - this.anchorY;
    }

    private static float snapQuarter(float yaw) {
        int quarter = Math.round(yaw / 90.0F);
        return quarter * 90.0F;
    }

    private float getInputSensitivityScale() {
        if (this.inputSensitivityIndex < 0 || this.inputSensitivityIndex >= INPUT_SENS_PRESETS.length) {
            this.inputSensitivityIndex = INPUT_SENS_DEFAULT_INDEX;
        }
        return INPUT_SENS_PRESETS[this.inputSensitivityIndex];
    }

    private static String normalizeCategory(String category) {
        if (category == null) {
            return CATEGORY_ALL;
        }
        String value = category.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || CATEGORY_ALL.equals(value)) {
            return CATEGORY_ALL;
        }
        if (value.startsWith(CATEGORY_MOD_PREFIX) || value.startsWith(CATEGORY_TAB_PREFIX)) {
            return value;
        }
        return CATEGORY_MOD_PREFIX + value;
    }

    public record StorageEntry(ItemStack stack, String itemId, long count, String mod, String name) {
    }

    public record FluidEntry(
            String fluidId,
            String label,
            long amount,
            long capacity,
            String mod,
            String name,
            ItemStack preview) {
    }

    public record FunnelBufferEntry(ItemStack stack, String itemId, long count) {
    }

    public record RecentEntry(
            boolean fluid,
            String id,
            String label,
            long amount,
            long capacity,
            byte kind,
            ItemStack preview) {
    }

    public record CraftRecipeOption(
            String recipeId,
            int resultCount,
            boolean craftable,
            String summary,
            String missingSummary) {
    }

    public record CraftFeedbackIngredient(
            String itemId,
            String label,
            ItemStack preview,
            int count) {
    }

    public record CraftableEntry(
            ItemStack stack,
            String recipeId,
            String itemId,
            int resultCount,
            boolean craftable,
            String missingSummary,
            String mod,
            String name,
            List<CraftRecipeOption> recipeOptions) {
    }

    public enum BuildShape {
        BLOCK,
        LINE,
        SQUARE,
        WALL,
        CIRCLE,
        BOX
    }

    private static RecentEntry decodeRecentEntry(String idText, long amount, long capacity, byte kind) {
        if (idText == null || idText.isBlank()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) {
            return null;
        }
        boolean fluidKind = kind == S2CRtsStoragePagePayload.RECENT_FLUID_PLACED
                || kind == S2CRtsStoragePagePayload.RECENT_FLUID_USED
                || kind == S2CRtsStoragePagePayload.RECENT_FLUID_CRAFTED;
        if (fluidKind) {
            if (!BuiltInRegistries.FLUID.containsKey(id)) {
                return null;
            }
            Fluid fluid = BuiltInRegistries.FLUID.get(id);
            FluidStack fluidStack = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
            ItemStack preview = FluidUtil.getFilledBucket(fluidStack);
            String label = fluid.getFluidType().getDescription(fluidStack).getString();
            return new RecentEntry(true, idText, label, Math.max(0L, amount), Math.max(0L, capacity), kind, preview);
        }
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        ItemStack preview = new ItemStack(BuiltInRegistries.ITEM.get(id));
        return new RecentEntry(false, idText, preview.getHoverName().getString(), Math.max(0L, amount), 0L, kind, preview);
    }

    private static final class ClientAlwaysValidContainer implements Container {
        private final Container delegate;

        private ClientAlwaysValidContainer(Container delegate) {
            this.delegate = delegate;
        }

        @Override
        public int getContainerSize() {
            return this.delegate.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return this.delegate.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.delegate.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return this.delegate.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return this.delegate.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.delegate.setItem(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return this.delegate.getMaxStackSize();
        }

        @Override
        public void setChanged() {
            this.delegate.setChanged();
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }

        @Override
        public void startOpen(net.minecraft.world.entity.player.Player player) {
            this.delegate.startOpen(player);
        }

        @Override
        public void stopOpen(net.minecraft.world.entity.player.Player player) {
            this.delegate.stopOpen(player);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return this.delegate.canPlaceItem(slot, stack);
        }

        @Override
        public void clearContent() {
            this.delegate.clearContent();
        }
    }

    private static final class ClientRelaxedContainerLevelAccess implements ContainerLevelAccess {
        private final ContainerLevelAccess delegate;

        private ClientRelaxedContainerLevelAccess(ContainerLevelAccess delegate) {
            this.delegate = delegate == null ? ContainerLevelAccess.NULL : delegate;
        }

        @Override
        public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> evaluator) {
            Optional<T> result = this.delegate.evaluate(evaluator);
            if (result.isPresent() && result.get() instanceof Boolean) {
                @SuppressWarnings("unchecked")
                T forcedTrue = (T) Boolean.TRUE;
                return Optional.of(forcedTrue);
            }
            return result;
        }

        @Override
        public void execute(BiConsumer<Level, BlockPos> consumer) {
            this.delegate.execute(consumer);
        }
    }
}



