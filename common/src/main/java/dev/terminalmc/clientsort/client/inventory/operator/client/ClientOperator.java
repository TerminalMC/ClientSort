/*
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

package dev.terminalmc.clientsort.client.inventory.operator.client;

import dev.terminalmc.clientsort.client.config.Operation;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.order.SortContext;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.util.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Provides methods for manipulating the player's inventory or open container via vanilla C2S
 * inventory interaction packets.
 */
public abstract class ClientOperator extends SingleUseOperator {

    public ClientOperator(AbstractContainerScreen<?> screen, Slot originSlot, Operation op) {
        super(screen, originSlot, op);
    }

    /**
     * Uses vanilla C2S inventory interaction packets to sort the inventory.
     */
    @Override
    protected void sort(SortOrder sortOrder) {
        if (sortOrder.equals(SortOrder.NONE))
            return;

        // Collect partial stacks
        collect();

        // Create an array of ascending slot numbers
        int[] key = new int[originScopeSlots.length];
        for (int i = 0; i < key.length; i++) {
            key[i] = i;
        }

        // Sort the array of slot numbers to create a sorting key which
        // defines, for each slot of originScopeSlots, the index in
        // originScopeStacks from which the new stack should be retrieved
        key = sortOrder.sort(
                key,
                originScopeStacks,
                new SortContext(Minecraft.getInstance().level)
        );

        // Prepare sounds
        boolean playSound = SoundManager.shouldPlaySortingSounds();
        if (playSound)
            SoundManager.resetForCount(SoundManager.estimateSortSounds(originScopeStacks));

        // Sort
        sort(key, playSound);
    }

    /**
     * Uses vanilla C2S inventory interaction packets to collect partial stacks into the smallest
     * possible number of slots.
     */
    protected abstract void collect();

    /**
     * Uses vanilla C2S inventory interaction packets to sort the inventory according to
     * {@code key}.
     */
    protected abstract void sort(int[] key, boolean playSound);
}
