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

package dev.terminalmc.clientsort.client.inventory.sort.client;

import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.sort.InventorySorter;
import dev.terminalmc.clientsort.client.order.SortContext;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import static dev.terminalmc.clientsort.client.config.Config.options;

public abstract class ClientSorter extends InventorySorter {
    public ClientSorter(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        super(screen, screenHelper, originSlot);
    }

    @Override
    public void sort(SortOrder sortOrder) {
        // Collect partial stacks
        collect();

        // Create an array of ascending slot numbers
        int[] key = new int[scopeSlots.length];
        for (int i = 0; i < key.length; i++) {
            key[i] = i;
        }
        // Sort the array of slot numbers to create a sorting key which
        // defines, for each slot of scopeSlots, which index of scopeSlots the
        // stack should be taken from
        key = sortOrder.sort(key, scopeStacks, new SortContext(Minecraft.getInstance().level));

        // Prepare sounds
        boolean playSound = options().soundEnabled && options().soundVolume > 0;
        if (playSound) SoundManager.resetForCount(estimateMaxSounds());

        // Sort
        sort(key, playSound);
    }

    /**
     * Estimates the maximum number of sounds for the sort.
     * <p>
     * Ideally pitch should reach maximum as sorting finishes, so we do a
     * quick calculation to roughly guess the number of sounds (and thus, the
     * number of pitch increments) needed.
     */
    private int estimateMaxSounds() {
        // Count non-empty stacks; assume all these require sorting
        int stackCount = 0;
        for (ItemStack stack : scopeStacks) {
            if (stack != ItemStack.EMPTY) {
                stackCount++;
            }
        }
        // Count 'holes' that will require filling
        int compaction = 0;
        for (int i = 0; i < stackCount; i++) {
            if (scopeStacks[i] == ItemStack.EMPTY) {
                compaction++;
            }
        }
        int size = stackCount + compaction;

        // Compensate for a small percentage of swaps requiring multiple clicks
        size += size / 15;

        return size;
    }

    /**
     * Uses C2S inventory interaction packets to collect partial stacks into
     * the smallest possible number of slots.
     */
    protected abstract void collect();

    /**
     * Uses C2S inventory interaction packets to sort the inventory according
     * to {@code key}.
     */
    protected abstract void sort(int[] key, boolean playSound);
}
