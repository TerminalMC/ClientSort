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

package dev.terminalmc.clientsort.client.inventory.sort.server;

import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.sort.InventorySorter;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.network.handler.CollectResultHandler;
import dev.terminalmc.clientsort.client.order.SortContext;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.Services;
import dev.terminalmc.clientsort.client.util.inject.ISlot;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Manages inventory sorting actions.
 */
public class ServerSorter extends InventorySorter {
    public ServerSorter(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        super(screen, screenHelper, originSlot);
    }

    @Override
    public void sort(SortOrder sortOrder) {
        CollectResultHandler.onSuccess = () -> {
            ServerSorter sorter = new ServerSorter(screen, screenHelper, originSlot);
            int[] slotMapping = sorter.createSlotMapping(sortOrder);
            sorter.sendSortPayload(slotMapping);
        };

        int[] scopeArray = createScopeArray();
        sendCollectPayload(scopeArray);
    }

    private int[] createScopeArray() {
        // Translate slots for server
        int[] slots = new int[scopeSlots.length];
        for (int i = 0; i < scopeSlots.length; i++) {
            slots[i] = ((ISlot) scopeSlots[i]).clientSort$getIdInContainer();
        }
        screenHelper.translateSlotIds(slots);
        return slots;
    }

    private void sendCollectPayload(int[] scopeArray) {
        InteractionManager.push(() -> {
            Services.PLATFORM.sendToServer(
                    new CollectPayload(screen.getMenu().containerId, scopeArray));
            return InteractionManager.TICK_WAITER;
        });
    }

    private int[] createSlotMapping(SortOrder sortOrder) {
        // Create an array of ascending slot numbers
        int[] sortedIds = new int[scopeStacks.length];
        for (int i = 0; i < sortedIds.length; i++) {
            sortedIds[i] = i;
        }

        // Sort the array of slot numbers to make a sorting 'key'
        sortedIds = sortOrder.sort(sortedIds, scopeStacks,
                new SortContext(Minecraft.getInstance().level));

        // Translate the key into a series of swap instructions
        int[] slotMapping = new int[sortedIds.length * 2];
        for (int i = 0; i < sortedIds.length; i++) {
            Slot from = scopeSlots[sortedIds[i]];
            Slot to = scopeSlots[i];
            slotMapping[i * 2] = ((ISlot) from).clientSort$getIdInContainer();
            slotMapping[i * 2 + 1] = ((ISlot) to).clientSort$getIdInContainer();
        }
        screenHelper.translateSlotIds(slotMapping);
        return slotMapping;
    }

    private void sendSortPayload(int[] slotMapping) {
        InteractionManager.push(() -> {
            Services.PLATFORM.sendToServer(
                    new SortPayload(screen.getMenu().containerId, slotMapping));
            return InteractionManager.TICK_WAITER;
        });
    }
}
