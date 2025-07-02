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

package dev.terminalmc.clientsort.client.inventory.control.server;

import dev.terminalmc.clientsort.client.inventory.control.SingleUseController;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.network.handler.CollectResultHandler;
import dev.terminalmc.clientsort.client.order.SortContext;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.ClientServices;
import dev.terminalmc.clientsort.client.util.inject.ISlot;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.TransferPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Provides methods for manipulating the player's inventory or open container via custom payload
 * packets.
 * <p>
 * Valid for use ONLY if the mod is also present server-side.
 */
public class ServerController extends SingleUseController {

    public ServerController(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        super(screen, screenHelper, originSlot);
    }

    @Override
    public void sort(SortOrder sortOrder) {
        if (!canOperate())
            return;
        CollectResultHandler.onSuccess = () -> {
            ServerController sorter = new ServerController(screen, screenHelper, originSlot);
            int[] slotMapping = sorter.createSlotMapping(sortOrder);
            InteractionManager.now(() -> {
                ClientServices.PLATFORM.sendToServer(
                        SortPayload.ID,
                        new SortPayload(
                                screen.getMenu().containerId,
                                slotMapping
                        )
                );
                return InteractionManager.TICK_WAITER;
            });
        };

        int[] scopeArray = createSlotIdArray(originScopeSlots);
        sendCollectPayload(scopeArray);
    }

    @Override
    public void fillStacks() {
        if (!canOperate())
            return;
        if (originScopeSlots.length == 0)
            return;
        if (otherScopeSlots.length == 0)
            return;

        int[] srcSlotIds = createSlotIdArray(originScopeSlots);
        int[] dstSlotIds = createSlotIdArray(otherScopeSlots);

        InteractionManager.now(() -> {
            ClientServices.PLATFORM.sendToServer(
                    StackFillPayload.ID,
                    new StackFillPayload(
                            screen.getMenu().containerId,
                            srcSlotIds,
                            dstSlotIds
                    )
            );
            return InteractionManager.TICK_WAITER;
        });
    }

    @Override
    public void transfer() {
        if (!canOperate())
            return;
        if (originScopeSlots.length == 0)
            return;
        if (otherScopeSlots.length == 0)
            return;

        int[] srcSlotIds = createSlotIdArray(originScopeSlots);
        int[] dstSlotIds = createSlotIdArray(otherScopeSlots);

        InteractionManager.now(() -> {
            ClientServices.PLATFORM.sendToServer(
                    TransferPayload.ID,
                    new TransferPayload(
                            screen.getMenu().containerId,
                            srcSlotIds,
                            dstSlotIds
                    )
            );
            return InteractionManager.TICK_WAITER;
        });
    }

    private int[] createSlotIdArray(Slot[] slots) {
        // Translate slots for server
        int[] slotIds = new int[slots.length];
        for (int i = 0; i < slots.length; i++) {
            slotIds[i] = ((ISlot) slots[i]).clientsort$getIdInContainer();
        }
        screenHelper.translateSlotIds(slotIds);
        return slotIds;
    }

    private void sendCollectPayload(int[] scopeArray) {
        InteractionManager.now(() -> {
            ClientServices.PLATFORM.sendToServer(
                    CollectPayload.ID,
                    new CollectPayload(
                            screen.getMenu().containerId,
                            scopeArray
                    )
            );
            return InteractionManager.TICK_WAITER;
        });
    }

    private int[] createSlotMapping(SortOrder sortOrder) {
        // Create an array of ascending slot numbers
        int[] sortedIds = new int[originScopeStacks.length];
        for (int i = 0; i < sortedIds.length; i++) {
            sortedIds[i] = i;
        }

        // Sort the array of slot numbers to make a sorting 'key'
        sortedIds = sortOrder.sort(
                sortedIds,
                originScopeStacks,
                new SortContext(Minecraft.getInstance().level)
        );

        // Translate the key into a series of swap instructions
        int[] slotMapping = new int[sortedIds.length * 2];
        for (int i = 0; i < sortedIds.length; i++) {
            Slot from = originScopeSlots[sortedIds[i]];
            Slot to = originScopeSlots[i];
            slotMapping[i * 2] = ((ISlot) from).clientsort$getIdInContainer();
            slotMapping[i * 2 + 1] = ((ISlot) to).clientsort$getIdInContainer();
        }
        screenHelper.translateSlotIds(slotMapping);
        return slotMapping;
    }
}
