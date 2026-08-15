/*
 * Copyright 2022 Siphalor
 * Copyright 2026 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.clientsort.client.order;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.mixin.client.accessor.CreativeModeTabsAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Allows the mod to store the creative inventory item order in memory to reduce compute load for
 * creative-order sort operations.
 */
public class CreativeSearchOrder {

    // Item order map
    private static final Object2IntMap<StackMatcher> stackPositionMap =
            new Object2IntOpenHashMap<>();

    static {
        stackPositionMap.defaultReturnValue(Integer.MAX_VALUE);
    }

    // Item order map lock
    private static final ReadWriteLock stackPositionMapLock = new ReentrantReadWriteLock();

    public static Lock getReadLock() {
        return stackPositionMapLock.readLock();
    }

    /**
     * <b>Note:</b> this method is NOT synchronized, callers MUST first acquire
     * {@link #getReadLock}.
     *
     * @return the creative inventory search order position of the specified item.
     */
    public static int getPositionUnsafe(ItemStack stack) {
        int pos = stackPositionMap.getOrDefault(StackMatcher.of(stack), Integer.MAX_VALUE);
        if (pos == Integer.MAX_VALUE) {
            pos = stackPositionMap.getOrDefault(StackMatcher.plain(stack), Integer.MAX_VALUE);
        }
        return pos;
    }

    /**
     * Clears {@link CreativeSearchOrder#stackPositionMap}, and re-populates it if possible and
     * configured to do so.
     */
    public static void tryRefreshStackPositionMap() {
        if (ClientSort.emiReloading) {
            ClientSort.updateBlockedByEmi = true;
            ClientSort.LOG.info("Search order update blocked by EMI reload, waiting...");
        } else {
            refreshStackPositionMap();
        }
    }

    /**
     * Clears and re-populates {@link CreativeSearchOrder#stackPositionMap} by looking up the
     * creative inventory.
     */
    private static void refreshStackPositionMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        FeatureFlagSet enabledFeatures = mc.level.enabledFeatures();

        Collection<ItemStack> displayStacks;
        try {
            CreativeModeTabs.tryRebuildTabContents(
                    enabledFeatures,
                    true,
                    mc.level.registryAccess()
            );

            // Other mods might modify these items while our thread is evaluating them,
            // so make a copy
            displayStacks = CreativeModeTabs.searchTab().getDisplayItems()
                    .stream().map(ItemStack::copy).toList();
        } finally {
            CreativeModeTabsAccessor.clientsort$setCachedParameters(null);
        }

        Thread.ofVirtual().name(ClientSort.MOD_ID + "-creative-sort-builder").start(
                () -> {
                    Lock writeLock = stackPositionMapLock.writeLock();
                    try {
                        writeLock.lock();
                        stackPositionMap.clear();
                        int i = 0;
                        for (ItemStack stack : displayStacks) {
                            stackPositionMap.put(StackMatcher.of(stack), i);
                            i++;
                        }
                    } finally {
                        writeLock.unlock();
                    }
                }
        );
    }
}
