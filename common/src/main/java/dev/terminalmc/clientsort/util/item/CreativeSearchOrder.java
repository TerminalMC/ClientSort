/*
 * Copyright 2022 Siphalor
 * Copyright 2025 TerminalMC
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

package dev.terminalmc.clientsort.util.item;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.main.MainSort;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Allows storing the creative inventory item order in memory to reduce compute
 * load for creative-order sort operations.
 */
public class CreativeSearchOrder {
    // Item order map
    private static final Object2IntMap<StackMatcher> stackPositionMap =
            new Object2IntOpenHashMap<>();
    static {
        stackPositionMap.defaultReturnValue(Integer.MAX_VALUE);
    }
    
    // Item order map lock
    private static final ReadWriteLock stackPositionMapLock =
            new ReentrantReadWriteLock();

    public static Lock getReadLock() {
        return stackPositionMapLock.readLock();
    }

    /**
     * @return the creative inventory search order position of the specified
     * item.
     */
    public static int getPosition(ItemStack stack) {
        int pos = stackPositionMap.getInt(StackMatcher.of(stack));
        if (pos == Integer.MAX_VALUE) {
            pos = stackPositionMap.getInt(StackMatcher.ignoreNbt(stack));
        }
        return pos;
    }

    /**
     * Clears {@link CreativeSearchOrder#stackPositionMap}, and re-populates it
     * if possible and configured to do so.
     */
    public static void tryRefreshStackPositionMap() {
        if (Config.options().optimizedCreativeSorting) {
            if (ClientSort.emiReloadLock.tryLock()) {
                refreshStackPositionMap();
                ClientSort.emiReloadLock.unlock();
            } else {
                MainSort.LOG.info("Search order update blocked by EMI reload, waiting...");
                ClientSort.updateBlockedByEmi = true;
            }
        } else {
            Lock lock = stackPositionMapLock.writeLock();
            lock.lock();
            stackPositionMap.clear();
            lock.unlock();
        }
    }

    /**
     * Clears and re-populates {@link CreativeSearchOrder#stackPositionMap} by
     * looking up the creative inventory.
     */
    private static void refreshStackPositionMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        FeatureFlagSet enabledFeatures = mc.level.enabledFeatures();
        boolean opTab = mc.player.canUseGameMasterBlocks()
                && mc.options.operatorItemsTab().get();

        CreativeModeTabs.tryRebuildTabContents(enabledFeatures, !opTab, mc.level.registryAccess());

        Collection<ItemStack> displayStacks = new ArrayList<>(
                CreativeModeTabs.searchTab().getDisplayItems());
        new Thread(() -> {
            Lock lock = stackPositionMapLock.writeLock();
            lock.lock();
            stackPositionMap.clear();
            if (displayStacks.isEmpty()) {
                lock.unlock();
                return;
            }

            int i = 0;
            for (ItemStack stack : displayStacks) {
                StackMatcher plainMatcher = StackMatcher.ignoreNbt(stack);
                if (!stack.hasFoil() || !stackPositionMap.containsKey(plainMatcher)) {
                    stackPositionMap.put(plainMatcher, i);
                    i++;
                }
                stackPositionMap.put(StackMatcher.of(stack), i);
                i++;
            }
            lock.unlock();
        },  MainSort.MOD_NAME + ": creative search stack position lookup builder").start();
    }
}
