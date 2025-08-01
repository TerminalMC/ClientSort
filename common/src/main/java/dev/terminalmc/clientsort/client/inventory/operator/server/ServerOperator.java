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

package dev.terminalmc.clientsort.client.inventory.operator.server;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.network.handler.CollectResultHandler;
import dev.terminalmc.clientsort.client.order.SortContext;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.ClientServices;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.TransferPayload;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import static dev.terminalmc.clientsort.ClientSort.debug;

/**
 * Provides methods for manipulating the player's inventory or open container via custom payload
 * packets.
 * <p>
 * Valid for use ONLY if the mod is also present server-side.
 */
public class ServerOperator<T extends Operation> extends SingleUseOperator<Operation> {

    public ServerOperator(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            T operation
    ) {
        super(screen, screenHelper, originSlot, operation);
    }

    @Override
    protected void sort(SortOrder sortOrder) {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation SORT: origin scope is empty!");
            return;
        }

        CollectResultHandler.onSuccess = () -> {
            ServerOperator<?> sorter = new ServerOperator<>(
                    screen,
                    screenHelper,
                    originSlot,
                    Operation.SORT
            );
            int[] slotMapping = sorter.createSlotMapping(sortOrder);
            InteractionManager.now(() -> {
                if (debug())
                    ClientSort.LOG.info("Sending payload for operation SORT");
                ClientServices.PLATFORM.sendToServer(new SortPayload(
                        screen.getMenu().containerId,
                        slotMapping
                ));
                return InteractionManager.TICK_WAITER;
            });
        };

        int[] scopeArray = createSlotIdArray(originScopeSlots);
        sendCollectPayload(scopeArray);
    }

    @Override
    protected void fillStacks() {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation STACK_FILL: origin scope is empty!");
            return;
        }
        if (otherScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation STACK_FILL: other scope is empty!");
            return;
        }

        int[] srcSlotIds = createSlotIdArray(originScopeSlots);
        int[] dstSlotIds = createSlotIdArray(otherScopeSlots);

        InteractionManager.now(() -> {
            if (debug())
                ClientSort.LOG.info("Sending payload for operation STACK_FILL");
            ClientServices.PLATFORM.sendToServer(new StackFillPayload(
                    screen.getMenu().containerId,
                    srcSlotIds,
                    dstSlotIds
            ));
            return InteractionManager.TICK_WAITER;
        });
    }

    @Override
    protected void transfer() {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation TRANSFER: origin scope is empty!");
            return;
        }
        if (otherScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation TRANSFER: other scope is empty!");
            return;
        }

        int[] srcSlotIds = createSlotIdArray(originScopeSlots);
        int[] dstSlotIds = createSlotIdArray(otherScopeSlots);

        InteractionManager.now(() -> {
            if (debug())
                ClientSort.LOG.info("Sending payload for operation TRANSFER");
            ClientServices.PLATFORM.sendToServer(new TransferPayload(
                    screen.getMenu().containerId,
                    srcSlotIds,
                    dstSlotIds
            ));
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
            if (debug())
                ClientSort.LOG.info("Sending payload for operation COLLECT");
            ClientServices.PLATFORM.sendToServer(new CollectPayload(
                    screen.getMenu().containerId,
                    scopeArray
            ));
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
