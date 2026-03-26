package com.rtsbuilding.rtsbuilding.compat.ae2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.items.IItemHandler;

public final class RtsAe2Compat {
    public interface ReportedCountItemHandler {
        long getReportedCount(int slot);
    }

    private static final Ae2Reflection REFLECTION = Ae2Reflection.tryLoad();

    private RtsAe2Compat() {
    }

    public static boolean isAvailable() {
        return REFLECTION != null;
    }

    public static IItemHandler createNetworkItemHandler(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null || REFLECTION == null) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        if (level == null || !level.hasChunkAt(pos)) {
            return null;
        }

        Object storageService = REFLECTION.findStorageService(level, pos);
        if (storageService == null) {
            return null;
        }
        return new Ae2NetworkItemHandler(player, storageService, REFLECTION);
    }

    public static long getReportedCount(IItemHandler handler, int slot, ItemStack fallbackStack) {
        if (handler instanceof ReportedCountItemHandler reported) {
            return Math.max(0L, reported.getReportedCount(slot));
        }
        return fallbackStack == null || fallbackStack.isEmpty() ? 0L : Math.max(0L, fallbackStack.getCount());
    }

    private static final class Ae2NetworkItemHandler implements IItemHandler, ReportedCountItemHandler {
        private final ServerPlayer player;
        private final Object storageService;
        private final Ae2Reflection reflection;
        private final List<SlotView> slots = new ArrayList<>();

        private Ae2NetworkItemHandler(ServerPlayer player, Object storageService, Ae2Reflection reflection) {
            this.player = player;
            this.storageService = storageService;
            this.reflection = reflection;
            refreshSnapshot();
        }

        @Override
        public int getSlots() {
            return this.slots.size() + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= this.slots.size()) {
                return ItemStack.EMPTY;
            }
            SlotView view = this.slots.get(slot);
            return view.amount() > 0L ? view.displayStack().copy() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            Object key = this.reflection.toItemKey(stack);
            if (key == null) {
                return stack.copy();
            }

            if (slot >= 0 && slot < this.slots.size()) {
                SlotView current = this.slots.get(slot);
                if (!this.reflection.keysEqual(current.key(), key)) {
                    return stack.copy();
                }
            } else if (slot != this.slots.size()) {
                return stack.copy();
            }

            long inserted = this.reflection.insert(this.storageService, key, stack.getCount(), this.player, simulate);
            if (inserted <= 0L) {
                return stack.copy();
            }

            if (!simulate) {
                recordInserted(key, inserted);
            }

            ItemStack remain = stack.copy();
            remain.shrink((int) Math.min(Integer.MAX_VALUE, inserted));
            return remain;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= this.slots.size() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            SlotView view = this.slots.get(slot);
            if (view.amount() <= 0L) {
                return ItemStack.EMPTY;
            }

            long extracted = this.reflection.extract(this.storageService, view.key(), amount, this.player, simulate);
            if (extracted <= 0L) {
                return ItemStack.EMPTY;
            }

            if (!simulate) {
                long nextAmount = Math.max(0L, view.amount() - extracted);
                this.slots.set(slot, new SlotView(view.key(), view.displayStack(), nextAmount));
            }

            return this.reflection.toStack(view.key(), (int) Math.min(Integer.MAX_VALUE, extracted));
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return this.reflection.toItemKey(stack) != null;
        }

        @Override
        public long getReportedCount(int slot) {
            if (slot < 0 || slot >= this.slots.size()) {
                return 0L;
            }
            return this.slots.get(slot).amount();
        }

        private void refreshSnapshot() {
            this.slots.clear();
            for (SlotView slot : this.reflection.snapshot(this.storageService)) {
                if (slot != null && slot.amount() > 0L && !slot.displayStack().isEmpty()) {
                    this.slots.add(slot);
                }
            }
        }

        private void recordInserted(Object key, long inserted) {
            for (int i = 0; i < this.slots.size(); i++) {
                SlotView current = this.slots.get(i);
                if (this.reflection.keysEqual(current.key(), key)) {
                    this.slots.set(i, new SlotView(key, current.displayStack(), current.amount() + inserted));
                    return;
                }
            }
            ItemStack display = this.reflection.toStack(key, 1);
            if (!display.isEmpty()) {
                display.setCount(1);
                this.slots.add(new SlotView(key, display, inserted));
            }
        }
    }

    private record SlotView(Object key, ItemStack displayStack, long amount) {
    }

    private static final class Ae2Reflection {
        private final BlockCapability<?, ?> inWorldGridNodeHostCapability;
        private final Method hostGetGridNode;
        private final Method gridNodeGetGrid;
        private final Method gridGetService;
        private final Class<?> storageServiceClass;
        private final Method storageServiceGetCachedInventory;
        private final Method storageServiceGetInventory;
        private final Method keyCounterIterator;
        private final Method keyEntryGetKey;
        private final Method keyEntryGetLongValue;
        private final Class<?> aeItemKeyClass;
        private final Method aeItemKeyOfStack;
        private final Method aeItemKeyToStack;
        private final Method meStorageInsert;
        private final Method meStorageExtract;
        private final Class<?> actionableClass;
        private final Object actionableSimulate;
        private final Object actionableModulate;
        private final Method actionSourceOfPlayer;

        private Ae2Reflection(
                BlockCapability<?, ?> inWorldGridNodeHostCapability,
                Method hostGetGridNode,
                Method gridNodeGetGrid,
                Method gridGetService,
                Class<?> storageServiceClass,
                Method storageServiceGetCachedInventory,
                Method storageServiceGetInventory,
                Method keyCounterIterator,
                Method keyEntryGetKey,
                Method keyEntryGetLongValue,
                Class<?> aeItemKeyClass,
                Method aeItemKeyOfStack,
                Method aeItemKeyToStack,
                Method meStorageInsert,
                Method meStorageExtract,
                Class<?> actionableClass,
                Object actionableSimulate,
                Object actionableModulate,
                Method actionSourceOfPlayer) {
            this.inWorldGridNodeHostCapability = inWorldGridNodeHostCapability;
            this.hostGetGridNode = hostGetGridNode;
            this.gridNodeGetGrid = gridNodeGetGrid;
            this.gridGetService = gridGetService;
            this.storageServiceClass = storageServiceClass;
            this.storageServiceGetCachedInventory = storageServiceGetCachedInventory;
            this.storageServiceGetInventory = storageServiceGetInventory;
            this.keyCounterIterator = keyCounterIterator;
            this.keyEntryGetKey = keyEntryGetKey;
            this.keyEntryGetLongValue = keyEntryGetLongValue;
            this.aeItemKeyClass = aeItemKeyClass;
            this.aeItemKeyOfStack = aeItemKeyOfStack;
            this.aeItemKeyToStack = aeItemKeyToStack;
            this.meStorageInsert = meStorageInsert;
            this.meStorageExtract = meStorageExtract;
            this.actionableClass = actionableClass;
            this.actionableSimulate = actionableSimulate;
            this.actionableModulate = actionableModulate;
            this.actionSourceOfPlayer = actionSourceOfPlayer;
        }

        private static Ae2Reflection tryLoad() {
            if (!ModList.get().isLoaded("ae2")) {
                return null;
            }

            try {
                Class<?> aeCapabilitiesClass = Class.forName("appeng.api.AECapabilities");
                Field inWorldField = aeCapabilitiesClass.getField("IN_WORLD_GRID_NODE_HOST");
                BlockCapability<?, ?> inWorldCapability = (BlockCapability<?, ?>) inWorldField.get(null);

                Class<?> hostClass = Class.forName("appeng.api.networking.IInWorldGridNodeHost");
                Method hostGetGridNode = hostClass.getMethod("getGridNode", Direction.class);

                Class<?> gridNodeClass = Class.forName("appeng.api.networking.IGridNode");
                Method gridNodeGetGrid = gridNodeClass.getMethod("getGrid");

                Class<?> gridClass = Class.forName("appeng.api.networking.IGrid");
                Class<?> storageServiceClass = Class.forName("appeng.api.networking.storage.IStorageService");
                Method gridGetService = gridClass.getMethod("getService", Class.class);

                Method storageServiceGetCachedInventory = storageServiceClass.getMethod("getCachedInventory");
                Method storageServiceGetInventory = storageServiceClass.getMethod("getInventory");

                Class<?> keyCounterClass = Class.forName("appeng.api.stacks.KeyCounter");
                Method keyCounterIterator = keyCounterClass.getMethod("iterator");

                Class<?> keyEntryClass = Class.forName("it.unimi.dsi.fastutil.objects.Object2LongMap$Entry");
                Method keyEntryGetKey = keyEntryClass.getMethod("getKey");
                Method keyEntryGetLongValue = keyEntryClass.getMethod("getLongValue");

                Class<?> aeItemKeyClass = Class.forName("appeng.api.stacks.AEItemKey");
                Method aeItemKeyOfStack = aeItemKeyClass.getMethod("of", ItemStack.class);
                Method aeItemKeyToStack = aeItemKeyClass.getMethod("toStack", int.class);

                Class<?> meStorageClass = Class.forName("appeng.api.storage.MEStorage");
                Class<?> aeKeyClass = Class.forName("appeng.api.stacks.AEKey");
                Class<?> actionableClass = Class.forName("appeng.api.config.Actionable");
                Class<?> actionSourceClass = Class.forName("appeng.api.networking.security.IActionSource");
                Method meStorageInsert = meStorageClass.getMethod("insert", aeKeyClass, long.class, actionableClass, actionSourceClass);
                Method meStorageExtract = meStorageClass.getMethod("extract", aeKeyClass, long.class, actionableClass, actionSourceClass);

                Object actionableSimulate = Enum.valueOf((Class<? extends Enum>) actionableClass.asSubclass(Enum.class), "SIMULATE");
                Object actionableModulate = Enum.valueOf((Class<? extends Enum>) actionableClass.asSubclass(Enum.class), "MODULATE");

                Method actionSourceOfPlayer = actionSourceClass.getMethod(
                        "ofPlayer",
                        Class.forName("net.minecraft.world.entity.player.Player"));

                return new Ae2Reflection(
                        inWorldCapability,
                        hostGetGridNode,
                        gridNodeGetGrid,
                        gridGetService,
                        storageServiceClass,
                        storageServiceGetCachedInventory,
                        storageServiceGetInventory,
                        keyCounterIterator,
                        keyEntryGetKey,
                        keyEntryGetLongValue,
                        aeItemKeyClass,
                        aeItemKeyOfStack,
                        aeItemKeyToStack,
                        meStorageInsert,
                        meStorageExtract,
                        actionableClass,
                        actionableSimulate,
                        actionableModulate,
                        actionSourceOfPlayer);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        private Object findStorageService(ServerLevel level, BlockPos pos) {
            Object host = level.getCapability((BlockCapability<Object, Void>) this.inWorldGridNodeHostCapability, pos, null);
            if (host == null) {
                return null;
            }

            for (Direction direction : Direction.values()) {
                Object node = invoke(this.hostGetGridNode, host, direction);
                Object storageService = resolveStorageService(node);
                if (storageService != null) {
                    return storageService;
                }
            }
            Object node = invoke(this.hostGetGridNode, host, new Object[] { null });
            return resolveStorageService(node);
        }

        private Object resolveStorageService(Object node) {
            if (node == null) {
                return null;
            }
            Object grid = invoke(this.gridNodeGetGrid, node);
            if (grid == null) {
                return null;
            }
            Object storageService = invoke(this.gridGetService, grid, this.storageServiceClass);
            return this.storageServiceClass.isInstance(storageService) ? storageService : null;
        }

        private List<SlotView> snapshot(Object storageService) {
            List<SlotView> out = new ArrayList<>();
            Object keyCounter = invoke(this.storageServiceGetCachedInventory, storageService);
            if (keyCounter == null) {
                return out;
            }

            Iterator<?> iterator = (Iterator<?>) invoke(this.keyCounterIterator, keyCounter);
            if (iterator == null) {
                return out;
            }

            while (iterator.hasNext()) {
                Object entry = iterator.next();
                Object key = invoke(this.keyEntryGetKey, entry);
                if (key == null || !this.aeItemKeyClass.isInstance(key)) {
                    continue;
                }
                long amount = asLong(invoke(this.keyEntryGetLongValue, entry));
                if (amount <= 0L) {
                    continue;
                }
                ItemStack display = toStack(key, 1);
                if (display.isEmpty()) {
                    continue;
                }
                display.setCount(1);
                out.add(new SlotView(key, display, amount));
            }
            return out;
        }

        private Object toItemKey(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            Object key = invoke(this.aeItemKeyOfStack, null, stack);
            return this.aeItemKeyClass.isInstance(key) ? key : null;
        }

        private ItemStack toStack(Object key, int count) {
            if (key == null || !this.aeItemKeyClass.isInstance(key) || count <= 0) {
                return ItemStack.EMPTY;
            }
            Object stack = invoke(this.aeItemKeyToStack, key, count);
            return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
        }

        private long insert(Object storageService, Object key, long amount, ServerPlayer player, boolean simulate) {
            if (storageService == null || key == null || amount <= 0L) {
                return 0L;
            }
            Object meStorage = invoke(this.storageServiceGetInventory, storageService);
            if (meStorage == null) {
                return 0L;
            }
            Object source = invoke(this.actionSourceOfPlayer, null, player);
            return asLong(invoke(
                    this.meStorageInsert,
                    meStorage,
                    key,
                    amount,
                    simulate ? this.actionableSimulate : this.actionableModulate,
                    source));
        }

        private long extract(Object storageService, Object key, long amount, ServerPlayer player, boolean simulate) {
            if (storageService == null || key == null || amount <= 0L) {
                return 0L;
            }
            Object meStorage = invoke(this.storageServiceGetInventory, storageService);
            if (meStorage == null) {
                return 0L;
            }
            Object source = invoke(this.actionSourceOfPlayer, null, player);
            return asLong(invoke(
                    this.meStorageExtract,
                    meStorage,
                    key,
                    amount,
                    simulate ? this.actionableSimulate : this.actionableModulate,
                    source));
        }

        private boolean keysEqual(Object left, Object right) {
            return left == right || (left != null && left.equals(right));
        }

        private static long asLong(Object value) {
            return value instanceof Number number ? number.longValue() : 0L;
        }

        private static Object invoke(Method method, Object target, Object... args) {
            if (method == null) {
                return null;
            }
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
