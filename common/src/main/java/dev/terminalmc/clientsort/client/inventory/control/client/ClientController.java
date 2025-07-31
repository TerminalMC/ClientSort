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

package dev.terminalmc.clientsort.client.inventory.control.client;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.inventory.control.SingleUseController;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.order.SortContext;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.inventory.Slot;

/**
 * Provides methods for manipulating the player's inventory or open container via vanilla C2S
 * inventory interaction packets.
 */
public abstract class ClientController extends SingleUseController {

    public ClientController(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            Type<?> type
    ) {
        super(screen, screenHelper, originSlot, type);
    }

    /**
     * Sets a flag to indicate that a client interaction operation is in progress.
     */
    protected void raiseFlag() {
        ClientSort.operatingClient = true;
    }

    /**
     * Queues an interaction event to clear the flag set by {@link ClientController#raiseFlag()}.
     */
    protected void lowerFlag() {
        InteractionManager.push(() -> {
            ClientSort.operatingClient = false;
            return InteractionManager.TICK_WAITER;
        });
    }

    /**
     * Uses vanilla C2S inventory interaction packets to sort the inventory.
     */
    @Override
    protected void sort(SortOrder sortOrder) {
        if (!canOperate())
            return;

        raiseFlag();

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

        lowerFlag();
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
