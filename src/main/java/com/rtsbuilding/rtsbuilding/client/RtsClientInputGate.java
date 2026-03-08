package com.rtsbuilding.rtsbuilding.client;

import java.util.ArrayList;
import java.util.List;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.network.C2SRtsCraftRefillPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsLinkedPickupPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsImportMenuSlotPayload;
import com.rtsbuilding.rtsbuilding.network.C2SRtsReturnCarriedPayload;
import com.rtsbuilding.rtsbuilding.network.RtsStorageSort;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RtsbuildingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class RtsClientInputGate {
    private static final int OVERLAY_MARGIN = 6;
    private static final int PANEL_W = 212;
    private static final int SLOT_PITCH = 20;
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int RETURN_SLOTS = 5;
    private static final int QUICKBAR_Y_OFF = 17;
    private static final int GRID_Y_OFF = QUICKBAR_Y_OFF + SLOT_SIZE + 6;
    private static final int RETURN_LABEL_Y_OFF = GRID_Y_OFF + ROWS * SLOT_PITCH + 2;
    private static final int RETURN_Y_OFF = RETURN_LABEL_Y_OFF + 9;
    private static final int OVERLAY_H = RETURN_Y_OFF + SLOT_SIZE + 6;
    private static final int OVERLAY_HEADER_Y = 3;
    private static final int OVERLAY_HEADER_H = 11;
    private static final int OVERLAY_SORT_X = 40;
    private static final int OVERLAY_DIR_X = OVERLAY_SORT_X + 14;
    private static final int OVERLAY_SEARCH_X = OVERLAY_DIR_X + 16;
    private static final int OVERLAY_SEARCH_CLEAR_W = 10;
    private static final int OVERLAY_SEARCH_MAX = 64;
    private static final long RETURN_PREVIEW_MS = 2000L;

    private static String pendingOverlayCarriedItemId = "";
    private static boolean captureLeftRelease;
    private static boolean captureRightRelease;
    private static boolean overlaySearchFocused;
    private static String overlaySearchDraft = "";
    private static Screen activeOverlayScreen;
    private static Screen pendingCraftRefillScreen;
    private static int pendingCraftRefillButton = -1;
    private static List<String> pendingCraftRefillBlueprint = List.of();
    private static final ItemStack[] RETURN_QUEUE = new ItemStack[RETURN_SLOTS];
    private static final long[] RETURN_QUEUE_EXPIRY = new long[RETURN_SLOTS];

    static {
        for (int i = 0; i < RETURN_SLOTS; i++) {
            RETURN_QUEUE[i] = ItemStack.EMPTY;
        }
    }

    private RtsClientInputGate() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (ClientRtsController.get().isEnabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!ClientRtsController.get().isEnabled()) {
            return;
        }

        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)
                || event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientRtsController.get().isEnabled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof BuilderScreen) {
            return;
        }
        if (event.getScreen() instanceof RtsCraftTerminalScreen) {
            return;
        }
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }

        syncOverlayScreen(event.getScreen());
        if (!ClientRtsController.get().canUseStorageOverlay()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();

        int panelX = Math.max(OVERLAY_MARGIN, (sw - PANEL_W) / 2);
        int panelY = resolvePanelY(event.getScreen(), sh);
        int panelW = PANEL_W;
        g.fill(panelX, panelY, panelX + panelW, panelY + OVERLAY_H, 0xBB0F1116);
        ClientRtsController controller = ClientRtsController.get();
        if (!overlaySearchFocused) {
            overlaySearchDraft = controller.getStorageSearch();
        }

        g.drawString(minecraft.font, "RTS", panelX + 6, panelY + 4, 0xFFFFFF);

        int headerY = panelY + OVERLAY_HEADER_Y;
        drawMiniButton(g, minecraft.font, panelX + OVERLAY_SORT_X, headerY, 12, OVERLAY_HEADER_H, sortShort(controller.getStorageSort()));
        drawMiniButton(g, minecraft.font, panelX + OVERLAY_DIR_X, headerY, 12, OVERLAY_HEADER_H,
                controller.isStorageSortAscending() ? "A" : "D");

        int pageX = panelX + panelW - 58;
        int searchX = panelX + OVERLAY_SEARCH_X;
        int searchW = Math.max(26, pageX - searchX - 3);
        int clearX = searchX + searchW - OVERLAY_SEARCH_CLEAR_W;
        int searchBg = overlaySearchFocused ? 0xAA304153 : 0xAA202731;
        g.fill(searchX, headerY, searchX + searchW, headerY + OVERLAY_HEADER_H, searchBg);
        g.hLine(searchX, searchX + searchW, headerY, 0xFF61758A);
        g.hLine(searchX, searchX + searchW, headerY + OVERLAY_HEADER_H, 0xFF10161D);
        g.vLine(searchX, headerY, headerY + OVERLAY_HEADER_H, 0xFF61758A);
        g.vLine(searchX + searchW, headerY, headerY + OVERLAY_HEADER_H, 0xFF10161D);

        String searchText = overlaySearchDraft == null ? "" : overlaySearchDraft;
        String display = trimToWidth(minecraft.font, searchText, Math.max(8, searchW - OVERLAY_SEARCH_CLEAR_W - 5));
        g.drawString(minecraft.font, display, searchX + 2, headerY + 2, 0xEAF2FF);
        if (overlaySearchFocused && (System.currentTimeMillis() / 300L) % 2L == 0L) {
            int caretX = searchX + 2 + minecraft.font.width(display) + 1;
            g.fill(caretX, headerY + 2, caretX + 1, headerY + OVERLAY_HEADER_H - 2, 0xFFEAF2FF);
        }
        g.fill(clearX, headerY, clearX + OVERLAY_SEARCH_CLEAR_W, headerY + OVERLAY_HEADER_H, 0xAA2A3340);
        g.drawCenteredString(minecraft.font, "x", clearX + OVERLAY_SEARCH_CLEAR_W / 2, headerY + 2,
                searchText.isEmpty() ? 0x88A0B4C8 : 0xFFFFFF);

        g.fill(pageX, panelY + 3, pageX + 12, panelY + 14, 0xAA2A2A2A);
        g.drawString(minecraft.font, "<", pageX + 4, panelY + 5, 0xFFFFFF);
        g.fill(pageX + 46, panelY + 3, pageX + 58, panelY + 14, 0xAA2A2A2A);
        g.drawString(minecraft.font, ">", pageX + 50, panelY + 5, 0xFFFFFF);
        g.drawString(minecraft.font, (controller.getStoragePage() + 1) + "/" + controller.getStorageTotalPages(), pageX + 15, panelY + 5, 0xDDDDDD);

        int quickbarX = panelX + 6;
        int quickbarY = panelY + QUICKBAR_Y_OFF;
        renderQuickbar(g, minecraft.font, quickbarX, quickbarY);

        int gridX = panelX + 6;
        int gridY = panelY + GRID_Y_OFF;
        var entries = controller.getStorageEntries();
        int maxSlots = Math.min(entries.size(), COLS * ROWS);
        for (int i = 0; i < COLS * ROWS; i++) {
            int cx = gridX + (i % COLS) * SLOT_PITCH;
            int cy = gridY + (i / COLS) * SLOT_PITCH;
            g.fill(cx, cy, cx + SLOT_SIZE, cy + SLOT_SIZE, 0xAA131313);
            if (i < maxSlots) {
                var entry = entries.get(i);
                g.renderItem(entry.stack(), cx + 1, cy + 1);
                drawSlotCountOverlay(g, minecraft.font, cx, cy, SLOT_SIZE, compactCount(entry.count()), 0xFFF7E6A8);
            }
        }

        pruneReturnQueue();
        int returnX = panelX + 6;
        int returnY = panelY + RETURN_Y_OFF;
        g.drawString(minecraft.font, "Return (drop to import)", returnX, panelY + RETURN_LABEL_Y_OFF, 0xD5E7FF);
        for (int i = 0; i < RETURN_SLOTS; i++) {
            int cx = returnX + i * SLOT_PITCH;
            int cy = returnY;
            g.fill(cx, cy, cx + SLOT_SIZE, cy + SLOT_SIZE, 0xAA20262E);
            g.hLine(cx, cx + SLOT_SIZE, cy, 0xFF4E5A67);
            g.hLine(cx, cx + SLOT_SIZE, cy + SLOT_SIZE, 0xFF161A20);
            g.vLine(cx, cy, cy + SLOT_SIZE, 0xFF4E5A67);
            g.vLine(cx + SLOT_SIZE, cy, cy + SLOT_SIZE, 0xFF161A20);

            ItemStack preview = RETURN_QUEUE[i];
            if (!preview.isEmpty()) {
                g.renderItem(preview, cx + 1, cy + 1);
                drawSlotCountOverlay(g, minecraft.font, cx, cy, SLOT_SIZE, compactCount(preview.getCount()), 0xFFE8F6FF);
            } else {
                g.drawString(minecraft.font, "+", cx + 6, cy + 5, 0xAACEE1FF);
            }
        }

    }

    @SubscribeEvent
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!ClientRtsController.get().canUseStorageOverlay()) {
            return;
        }
        if (event.getScreen() instanceof BuilderScreen) {
            return;
        }
        if (event.getScreen() instanceof RtsCraftTerminalScreen) {
            return;
        }
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int panelX = Math.max(OVERLAY_MARGIN, (sw - PANEL_W) / 2);
        int panelY = resolvePanelY(event.getScreen(), sh);
        int pageX = panelX + PANEL_W - 58;
        int headerY = panelY + OVERLAY_HEADER_Y;
        int sortX = panelX + OVERLAY_SORT_X;
        int dirX = panelX + OVERLAY_DIR_X;
        int searchX = panelX + OVERLAY_SEARCH_X;
        int searchW = Math.max(26, pageX - searchX - 3);
        int clearX = searchX + searchW - OVERLAY_SEARCH_CLEAR_W;

        double mx = event.getMouseX();
        double my = event.getMouseY();
        capturePendingCraftRefill((AbstractContainerScreen<?>) event.getScreen(), mx, my, event.getButton());
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (Screen.hasShiftDown()) {
                if (tryImportHoveredMenuSlot((AbstractContainerScreen<?>) event.getScreen(), mx, my, event.getButton())) {
                    captureLeftRelease = true;
                    event.setCanceled(true);
                    return;
                }
            }
            if (!inside(mx, my, panelX, panelY, PANEL_W, OVERLAY_H)) {
                overlaySearchFocused = false;
                return;
            }
            if (inside(mx, my, sortX, headerY, 12, OVERLAY_HEADER_H)) {
                ClientRtsController.get().cycleSort();
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }
            if (inside(mx, my, dirX, headerY, 12, OVERLAY_HEADER_H)) {
                ClientRtsController.get().toggleSortDirection();
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }
            if (inside(mx, my, clearX, headerY, OVERLAY_SEARCH_CLEAR_W, OVERLAY_HEADER_H)) {
                overlaySearchDraft = "";
                overlaySearchFocused = false;
                ClientRtsController.get().setStorageSearch("");
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }
            if (inside(mx, my, searchX, headerY, searchW, OVERLAY_HEADER_H)) {
                overlaySearchFocused = true;
                overlaySearchDraft = ClientRtsController.get().getStorageSearch();
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }
            overlaySearchFocused = false;
            int quickbarIdx = resolveQuickbarSlotIndex(mx, my, panelX + 6, panelY + QUICKBAR_Y_OFF);
            if (quickbarIdx >= 0) {
                selectOverlayQuickbarSlot(quickbarIdx);
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }
            if (inside(mx, my, pageX, panelY + 3, 12, 11)) {
                ClientRtsController.get().prevPage();
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }
            if (inside(mx, my, pageX + 46, panelY + 3, 12, 11)) {
                ClientRtsController.get().nextPage();
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }

            int returnIdx = resolveReturnSlotIndex(mx, my, panelX + 6, panelY + RETURN_Y_OFF);
            if (returnIdx >= 0) {
                tryDepositCarriedToLinked(Integer.MAX_VALUE);
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }

            int idx = resolveOverlaySlotIndex(mx, my, panelX + 6, panelY + GRID_Y_OFF);
            if (!minecraft.player.containerMenu.getCarried().isEmpty()
                    && idx >= 0
                    && tryDepositCarriedToLinked(Integer.MAX_VALUE)) {
                captureLeftRelease = true;
                event.setCanceled(true);
                return;
            }
            if (tryPickupFromOverlay(idx, Integer.MAX_VALUE)) {
                captureLeftRelease = true;
                event.setCanceled(true);
            }
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (Screen.hasShiftDown()) {
                if (tryImportHoveredMenuSlot((AbstractContainerScreen<?>) event.getScreen(), mx, my, event.getButton())) {
                    captureRightRelease = true;
                    event.setCanceled(true);
                    return;
                }
            }

            int returnIdx = resolveReturnSlotIndex(mx, my, panelX + 6, panelY + RETURN_Y_OFF);
            if (returnIdx >= 0) {
                tryDepositCarriedToLinked(1);
                captureRightRelease = true;
                event.setCanceled(true);
                return;
            }

            int idx = resolveOverlaySlotIndex(mx, my, panelX + 6, panelY + GRID_Y_OFF);
            if (!minecraft.player.containerMenu.getCarried().isEmpty()
                    && idx >= 0
                    && tryDepositCarriedToLinked(1)) {
                captureRightRelease = true;
                event.setCanceled(true);
                return;
            }
            if (tryPickupFromOverlay(idx, 1)) {
                captureRightRelease = true;
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!ClientRtsController.get().canUseStorageOverlay()
                || event.getScreen() instanceof BuilderScreen
                || event.getScreen() instanceof RtsCraftTerminalScreen
                || !(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            captureLeftRelease = false;
            captureRightRelease = false;
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && captureLeftRelease) {
            captureLeftRelease = false;
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && captureRightRelease) {
            captureRightRelease = false;
            event.setCanceled(true);
            return;
        }

        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        trySendPendingCraftRefill(event.getScreen(), event.getButton());

        // Click-to-pick / click-to-return is handled on mouse press so the carried item does not snap back on release.
    }

    @SubscribeEvent
    public static void onScreenMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!ClientRtsController.get().canUseStorageOverlay()) {
            return;
        }
        if (event.getScreen() instanceof BuilderScreen) {
            return;
        }
        if (event.getScreen() instanceof RtsCraftTerminalScreen) {
            return;
        }
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        int panelX = Math.max(OVERLAY_MARGIN, (sw - PANEL_W) / 2);
        int panelY = resolvePanelY(event.getScreen(), sh);
        if (!inside(event.getMouseX(), event.getMouseY(), panelX, panelY, PANEL_W, OVERLAY_H)) {
            return;
        }

        if (event.getScrollDeltaY() > 0.0D) {
            ClientRtsController.get().prevPage();
        } else if (event.getScrollDeltaY() < 0.0D) {
            ClientRtsController.get().nextPage();
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!ClientRtsController.get().canUseStorageOverlay()
                || event.getScreen() instanceof BuilderScreen
                || event.getScreen() instanceof RtsCraftTerminalScreen
                || !(event.getScreen() instanceof AbstractContainerScreen<?>)
                || !overlaySearchFocused) {
            return;
        }

        int keyCode = event.getKeyCode();
        boolean ctrl = (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            overlaySearchDraft = "";
            overlaySearchFocused = false;
            ClientRtsController.get().setStorageSearch("");
            event.setCanceled(true);
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            overlaySearchFocused = false;
            event.setCanceled(true);
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!overlaySearchDraft.isEmpty()) {
                overlaySearchDraft = overlaySearchDraft.substring(0, overlaySearchDraft.length() - 1);
                ClientRtsController.get().setStorageSearch(overlaySearchDraft);
            }
            event.setCanceled(true);
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            overlaySearchDraft = "";
            ClientRtsController.get().setStorageSearch("");
            event.setCanceled(true);
            return;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            Minecraft minecraft = Minecraft.getInstance();
            String clip = minecraft.keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                appendSearchText(clip);
            }
            event.setCanceled(true);
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!ClientRtsController.get().canUseStorageOverlay()
                || event.getScreen() instanceof BuilderScreen
                || event.getScreen() instanceof RtsCraftTerminalScreen
                || !(event.getScreen() instanceof AbstractContainerScreen<?>)
                || !overlaySearchFocused) {
            return;
        }
        int codePoint = event.getCodePoint();
        if (!Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint)) {
            event.setCanceled(true);
            return;
        }
        appendSearchText(new String(Character.toChars(codePoint)));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        captureLeftRelease = false;
        captureRightRelease = false;
        overlaySearchFocused = false;
        overlaySearchDraft = "";
        activeOverlayScreen = null;
        clearPendingCraftRefill();
        if (!ClientRtsController.get().canUseStorageOverlay()) {
            pendingOverlayCarriedItemId = "";
            return;
        }
        if (event.getScreen() instanceof BuilderScreen) {
            return;
        }
        if (event.getScreen() instanceof RtsCraftTerminalScreen) {
            pendingOverlayCarriedItemId = "";
            return;
        }
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            pendingOverlayCarriedItemId = "";
            return;
        }

        if (pendingOverlayCarriedItemId.isBlank()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            pendingOverlayCarriedItemId = "";
            return;
        }

        ItemStack carried = minecraft.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            pendingOverlayCarriedItemId = "";
            return;
        }

        var carriedId = BuiltInRegistries.ITEM.getKey(carried.getItem());
        if (carriedId == null || !pendingOverlayCarriedItemId.equals(carriedId.toString())) {
            pendingOverlayCarriedItemId = "";
            return;
        }

        PacketDistributor.sendToServer(new C2SRtsReturnCarriedPayload(pendingOverlayCarriedItemId, carried.getCount()));
        minecraft.player.containerMenu.setCarried(ItemStack.EMPTY);
        pendingOverlayCarriedItemId = "";
    }

    private static int resolvePanelY(net.minecraft.client.gui.screens.Screen screen, int screenHeight) {
        if (screen instanceof CraftingScreen || screen instanceof RtsCraftTerminalScreen) {
            return OVERLAY_MARGIN;
        }
        return screenHeight - OVERLAY_H - OVERLAY_MARGIN;
    }

    private static void drawMiniButton(GuiGraphics g, Font font, int x, int y, int w, int h, String label) {
        g.fill(x, y, x + w, y + h, 0xAA2B3642);
        g.hLine(x, x + w, y, 0xFF667D95);
        g.hLine(x, x + w, y + h, 0xFF111821);
        g.vLine(x, y, y + h, 0xFF667D95);
        g.vLine(x + w, y, y + h, 0xFF111821);
        g.drawCenteredString(font, label, x + w / 2, y + 2, 0xFFFFFF);
    }

    private static String sortShort(RtsStorageSort sort) {
        return switch (sort) {
            case QUANTITY -> "Q";
            case MOD -> "M";
            case NAME -> "N";
        };
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String ellipsis = "...";
        int limit = Math.max(0, maxWidth - font.width(ellipsis));
        int cut = text.length();
        while (cut > 0 && font.width(text.substring(0, cut)) > limit) {
            cut--;
        }
        return text.substring(0, cut) + ellipsis;
    }

    private static void appendSearchText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(overlaySearchDraft == null ? "" : overlaySearchDraft);
        for (int i = 0; i < raw.length() && sb.length() < OVERLAY_SEARCH_MAX; i++) {
            char ch = raw.charAt(i);
            if (Character.isISOControl(ch)) {
                continue;
            }
            sb.append(ch);
        }
        String next = sb.toString();
        if (!next.equals(overlaySearchDraft)) {
            overlaySearchDraft = next;
            ClientRtsController.get().setStorageSearch(overlaySearchDraft);
        }
    }

    private static void syncOverlayScreen(Screen screen) {
        if (screen == activeOverlayScreen) {
            return;
        }
        activeOverlayScreen = screen;
        overlaySearchFocused = false;
        overlaySearchDraft = "";
        ClientRtsController.get().requestStoragePage(ClientRtsController.get().getStoragePage());
        if (!ClientRtsController.get().getStorageSearch().isEmpty()) {
            ClientRtsController.get().setStorageSearch("");
        }
    }

    private static void capturePendingCraftRefill(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        clearPendingCraftRefill();
        if (screen == null || Screen.hasShiftDown()) {
            return;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }
        if (!(screen.getMenu() instanceof CraftingMenu menu)) {
            return;
        }
        int slotIndex = resolveHoveredMenuSlot(screen, mouseX, mouseY);
        if (slotIndex != 0) {
            return;
        }

        List<String> blueprint = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            Slot slot = menu.getSlot(1 + i);
            ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
            var itemId = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
            blueprint.add(itemId == null ? "" : itemId.toString());
        }

        pendingCraftRefillScreen = screen;
        pendingCraftRefillButton = button;
        pendingCraftRefillBlueprint = blueprint;
    }

    private static void trySendPendingCraftRefill(Screen screen, int button) {
        if (pendingCraftRefillScreen != screen
                || pendingCraftRefillButton != button
                || pendingCraftRefillBlueprint.size() != 9) {
            clearPendingCraftRefill();
            return;
        }
        PacketDistributor.sendToServer(new C2SRtsCraftRefillPayload(new ArrayList<>(pendingCraftRefillBlueprint)));
        clearPendingCraftRefill();
    }

    private static void clearPendingCraftRefill() {
        pendingCraftRefillScreen = null;
        pendingCraftRefillButton = -1;
        pendingCraftRefillBlueprint = List.of();
    }

    private static int resolveOverlaySlotIndex(double mouseX, double mouseY, int gridX, int gridY) {
        if (!inside(mouseX, mouseY, gridX, gridY, COLS * SLOT_PITCH, ROWS * SLOT_PITCH)) {
            return -1;
        }
        int col = Mth.floor((mouseX - gridX) / SLOT_PITCH);
        int row = Mth.floor((mouseY - gridY) / SLOT_PITCH);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return -1;
        }
        return row * COLS + col;
    }

    private static int resolveQuickbarSlotIndex(double mouseX, double mouseY, int x, int y) {
        if (!inside(mouseX, mouseY, x, y, COLS * SLOT_PITCH, SLOT_SIZE)) {
            return -1;
        }
        int col = Mth.floor((mouseX - x) / SLOT_PITCH);
        if (col < 0 || col >= COLS) {
            return -1;
        }
        int slotX = x + col * SLOT_PITCH;
        return mouseX <= slotX + SLOT_SIZE ? col : -1;
    }

    private static boolean tryImportHoveredMenuSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        int menuSlot = resolveHoveredMenuSlot(screen, mouseX, mouseY);
        if (menuSlot < 0) {
            return false;
        }

        // Unified behavior: crafting result slot uses Shift + LMB only.
        if (menuSlot == 0 && button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || screen.getMenu() == null || menuSlot >= screen.getMenu().slots.size()) {
            return false;
        }
        Slot slot = screen.getMenu().slots.get(menuSlot);
        if (slot == null || !slot.hasItem() || !slot.mayPickup(minecraft.player)) {
            return false;
        }

        PacketDistributor.sendToServer(new C2SRtsImportMenuSlotPayload(menuSlot));
        return true;
    }

    private static int resolveHoveredMenuSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (screen == null || screen.getMenu() == null) {
            return -1;
        }
        Slot hovered = screen.getSlotUnderMouse();
        if (hovered != null && hovered.hasItem()) {
            return screen.getMenu().slots.indexOf(hovered);
        }
        for (int i = 0; i < screen.getMenu().slots.size(); i++) {
            Slot slot = screen.getMenu().slots.get(i);
            int sx = screen.getGuiLeft() + slot.x;
            int sy = screen.getGuiTop() + slot.y;
            if (inside(mouseX, mouseY, sx, sy, SLOT_SIZE, SLOT_SIZE) && slot.hasItem()) {
                return i;
            }
        }
        return -1;
    }

    private static int resolveReturnSlotIndex(double mouseX, double mouseY, int x, int y) {
        if (!inside(mouseX, mouseY, x, y, RETURN_SLOTS * SLOT_PITCH, SLOT_SIZE)) {
            return -1;
        }
        int col = Mth.floor((mouseX - x) / SLOT_PITCH);
        if (col < 0 || col >= RETURN_SLOTS) {
            return -1;
        }
        int cx = x + col * SLOT_PITCH;
        return mouseX <= cx + SLOT_SIZE ? col : -1;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static String compactCount(long value) {
        if (value >= 1_000_000L) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000L) {
            return String.format("%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private static void drawSlotCountOverlay(GuiGraphics g, net.minecraft.client.gui.Font font, int slotX, int slotY,
            int slotSize, String countText, int color) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        g.fill(slotX + 1, slotY + slotSize - 8, slotX + slotSize - 1, slotY + slotSize - 1, 0xB0000000);
        g.drawString(font, countText, slotX + 2, slotY + slotSize - 7, color, true);
        g.pose().popPose();
    }

    private static void applyLocalCarriedPreview(ItemStack pickedPrototype, int requested) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || pickedPrototype.isEmpty()) {
            return;
        }

        ItemStack carried = minecraft.player.containerMenu.getCarried();
        int wanted = Math.max(1, requested);
        if (carried.isEmpty()) {
            ItemStack preview = pickedPrototype.copy();
            preview.setCount(Math.min(wanted, preview.getMaxStackSize()));
            minecraft.player.containerMenu.setCarried(preview);
            return;
        }

        if (!ItemStack.isSameItemSameComponents(carried, pickedPrototype)) {
            return;
        }
        int grow = Math.min(wanted, carried.getMaxStackSize() - carried.getCount());
        if (grow <= 0) {
            return;
        }
        carried.grow(grow);
        minecraft.player.containerMenu.setCarried(carried);
    }

    private static void renderQuickbar(GuiGraphics g, Font font, int x, int y) {
        ClientRtsController controller = ClientRtsController.get();
        for (int i = 0; i < COLS; i++) {
            int cx = x + i * SLOT_PITCH;
            int cy = y;
            ItemStack preview = controller.getQuickSlotPreview(i);
            String itemId = controller.getQuickSlotItemId(i);
            boolean filled = itemId != null && !itemId.isBlank();
            int bg = filled ? 0xAA253043 : 0xAA1A1A1A;
            g.fill(cx, cy, cx + SLOT_SIZE, cy + SLOT_SIZE, bg);
            g.hLine(cx, cx + SLOT_SIZE, cy, 0xFF67758A);
            g.hLine(cx, cx + SLOT_SIZE, cy + SLOT_SIZE, 0xFF0C0D10);
            g.vLine(cx, cy, cy + SLOT_SIZE, 0xFF67758A);
            g.vLine(cx + SLOT_SIZE, cy, cy + SLOT_SIZE, 0xFF0C0D10);

            if (!preview.isEmpty()) {
                g.renderItem(preview, cx + 1, cy + 1);
                if (itemId.equals(controller.getSelectedItemId())) {
                    g.fill(cx + 1, cy + 1, cx + SLOT_SIZE - 1, cy + SLOT_SIZE - 1, 0x3340FF80);
                }
                drawSlotCountOverlay(g, font, cx, cy, SLOT_SIZE, compactCount(resolvePinnedItemCount(itemId)), 0xFFF7E6A8);
            } else {
                g.drawCenteredString(font, Integer.toString(i + 1), cx + SLOT_SIZE / 2, cy + 5, 0x88D0D8E4);
            }
        }
    }

    private static long resolvePinnedItemCount(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0L;
        }
        for (ClientRtsController.StorageEntry entry : ClientRtsController.get().getStorageEntries()) {
            if (itemId.equals(entry.itemId())) {
                return entry.count();
            }
        }
        return 0L;
    }

    private static void selectOverlayQuickbarSlot(int index) {
        if (index < 0 || index >= COLS) {
            return;
        }
        ClientRtsController.get().selectQuickSlot(index);
    }

    private static boolean tryPickupFromOverlay(int index, int requestedAmount) {
        if (index < 0 || index >= ClientRtsController.get().getStorageEntries().size()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }

        var entry = ClientRtsController.get().getStorageEntries().get(index);
        ItemStack carried = minecraft.player.containerMenu.getCarried();
        int wanted = requestedFromCarried(carried, entry.stack(), requestedAmount);
        if (wanted <= 0) {
            return false;
        }

        applyLocalCarriedPreview(entry.stack(), wanted);
        PacketDistributor.sendToServer(new C2SRtsLinkedPickupPayload(entry.itemId(), wanted));
        ClientRtsController.get().selectStorageEntry(index);
        pendingOverlayCarriedItemId = entry.itemId();
        return true;
    }

    private static boolean tryDepositCarriedToLinked(int requestedAmount) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        ItemStack carried = minecraft.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return false;
        }
        var itemId = BuiltInRegistries.ITEM.getKey(carried.getItem());
        if (itemId == null) {
            return false;
        }

        int amount = Math.max(1, Math.min(requestedAmount, carried.getCount()));
        PacketDistributor.sendToServer(new C2SRtsReturnCarriedPayload(itemId.toString(), amount));

        ItemStack preview = carried.copy();
        preview.setCount(amount);
        enqueueReturnPreview(preview);

        carried.shrink(amount);
        if (carried.isEmpty()) {
            minecraft.player.containerMenu.setCarried(ItemStack.EMPTY);
        } else {
            minecraft.player.containerMenu.setCarried(carried);
        }

        pendingOverlayCarriedItemId = "";
        return true;
    }

    private static int requestedFromCarried(ItemStack carried, ItemStack target, int requestedAmount) {
        int requested = requestedAmount <= 0 ? 1 : requestedAmount;
        if (carried.isEmpty()) {
            return Math.min(requested, target.getMaxStackSize());
        }
        if (!ItemStack.isSameItemSameComponents(carried, target)) {
            return 0;
        }
        return Math.min(requested, carried.getMaxStackSize() - carried.getCount());
    }

    private static void enqueueReturnPreview(ItemStack stack) {
        pruneReturnQueue();
        int slot = -1;
        for (int i = 0; i < RETURN_SLOTS; i++) {
            if (RETURN_QUEUE[i].isEmpty()) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            for (int i = 1; i < RETURN_SLOTS; i++) {
                RETURN_QUEUE[i - 1] = RETURN_QUEUE[i];
                RETURN_QUEUE_EXPIRY[i - 1] = RETURN_QUEUE_EXPIRY[i];
            }
            slot = RETURN_SLOTS - 1;
        }
        RETURN_QUEUE[slot] = stack.copy();
        RETURN_QUEUE_EXPIRY[slot] = System.currentTimeMillis() + RETURN_PREVIEW_MS;
    }

    private static void pruneReturnQueue() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < RETURN_SLOTS; i++) {
            if (!RETURN_QUEUE[i].isEmpty() && now >= RETURN_QUEUE_EXPIRY[i]) {
                RETURN_QUEUE[i] = ItemStack.EMPTY;
                RETURN_QUEUE_EXPIRY[i] = 0L;
            }
        }

        int write = 0;
        for (int read = 0; read < RETURN_SLOTS; read++) {
            if (RETURN_QUEUE[read].isEmpty()) {
                continue;
            }
            if (write != read) {
                RETURN_QUEUE[write] = RETURN_QUEUE[read];
                RETURN_QUEUE_EXPIRY[write] = RETURN_QUEUE_EXPIRY[read];
                RETURN_QUEUE[read] = ItemStack.EMPTY;
                RETURN_QUEUE_EXPIRY[read] = 0L;
            }
            write++;
        }
    }

}
