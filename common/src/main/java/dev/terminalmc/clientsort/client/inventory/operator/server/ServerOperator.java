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

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.Operation;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.network.handler.CollectResultHandler;
import dev.terminalmc.clientsort.client.network.handler.SortResultHandler;
import dev.terminalmc.clientsort.client.network.handler.StackFillResultHandler;
import dev.terminalmc.clientsort.client.network.handler.TransferResultHandler;
import dev.terminalmc.clientsort.client.order.SortContext;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.services.ClientPlatformServices;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.TransferPayload;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

/**
 * Provides methods for manipulating the player's inventory or open container via custom payload
 * packets.
 * <p>
 * Valid for use ONLY if the mod is also present server-side.
 */
public class ServerOperator extends SingleUseOperator {

    public ServerOperator(AbstractContainerScreen<?> screen, Slot originSlot, Operation op) {
        super(screen, originSlot, op);
    }

    @Override
    protected void sort(SortOrder sortOrder) {
        if (sortOrder.equals(SortOrder.NONE))
            return;

        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation SORT: origin scope is empty!");
            return;
        }

        String id = UUID.randomUUID().toString();
        CollectResultHandler.onCompletion.put(
                id, (collectResult) -> {
            if (collectResult.isSuccess()) {
                SortResultHandler.onCompletion = (sortResult) -> {
                    if (!sortResult.isSuccess()) {
                        if (sortResult.isUnknown() || !options().useClientFallback) {
                            setOverlayMessage(Component.translatable(sortResult.translationKey));
                        } else {
                            SingleUseOperator.sort(screen, originSlot, true, sortOrder);
                        }
                    }
                };

                ServerOperator sorter = new ServerOperator(screen, originSlot, op);
                int[] slotMapping = sorter.createSlotMapping(sortOrder);
                if (debug())
                    ClientSort.LOG.info("Sending payload for operation SORT");
                ClientPlatformServices.getInstance().sendToServer(new SortPayload(
                        screen.getMenu().containerId,
                        slotMapping
                ));
            } else if (collectResult.isUnknown() || !options().useClientFallback) {
                setOverlayMessage(Component.translatable(collectResult.translationKey));
            } else {
                SingleUseOperator.sort(screen, originSlot, true, sortOrder);
            }
                }
        );
        ClientSort.taskManager.schedule(20, () -> CollectResultHandler.onCompletion.remove(id));

        int[] scopeArray = createSlotIdArray(originScopeSlots);
        sendCollectPayload(scopeArray, id);
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

        StackFillResultHandler.onCompletion = (result) -> {
            if (!result.isSuccess()) {
                if (result.isUnknown() || !options().useClientFallback) {
                    setOverlayMessage(Component.translatable(result.translationKey));
                } else {
                    SingleUseOperator.fillStacks(screen, originSlot, true);
                }
            }
        };

        int[] srcSlotIds = createSlotIdArray(originScopeSlots);
        int[] dstSlotIds = createSlotIdArray(otherScopeSlots);

        if (debug())
            ClientSort.LOG.info("Sending payload for operation STACK_FILL");
        ClientPlatformServices.getInstance().sendToServer(new StackFillPayload(
                screen.getMenu().containerId,
                srcSlotIds,
                dstSlotIds
        ));
    }

    @Override
    protected void matchTransfer() {
        transfer(collectMatchingSlots(
                originScopeSlots,
                otherScopeStacks,
                options().alwaysMatchByType,
                options().typeMatchItemCache
        ));
    }

    @Override
    protected void transfer() {
        transfer(null);
    }

    private void transfer(@Nullable Slot[] overrideSlots) {
        Slot[] originSlots = overrideSlots != null ? overrideSlots : originScopeSlots;
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation TRANSFER: origin scope is empty!");
            return;
        }
        if (originSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation TRANSFER: origin slots is empty!");
            return;
        }
        if (otherScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation TRANSFER: other scope is empty!");
            return;
        }

        TransferResultHandler.onCompletion = (result) -> {
            if (!result.isSuccess()) {
                if (result.isUnknown() || !options().useClientFallback) {
                    setOverlayMessage(Component.translatable(result.translationKey));
                } else {
                    if (overrideSlots != null) {
                        SingleUseOperator.transferMatching(screen, originSlot, true);
                    } else {
                        SingleUseOperator.transfer(screen, originSlot, true);
                    }
                }
            }
        };

        int[] srcSlotIds = createSlotIdArray(originSlots);
        int[] dstSlotIds = createSlotIdArray(otherScopeSlots);

        if (debug())
            ClientSort.LOG.info("Sending payload for operation TRANSFER");
        ClientPlatformServices.getInstance().sendToServer(new TransferPayload(
                screen.getMenu().containerId,
                srcSlotIds,
                dstSlotIds
        ));
    }

    private int[] createSlotIdArray(Slot[] slots) {
        // Translate slots for server
        int[] slotIds = new int[slots.length];
        for (int i = 0; i < slots.length; i++) {
            slotIds[i] = ((ISlot) slots[i]).clientsort$getIndexInMenu();
        }
        screenHelper.translateSlotIds(slotIds);
        return slotIds;
    }

    private void sendCollectPayload(int[] scopeArray, String id) {
        if (debug())
            ClientSort.LOG.info("Sending payload for operation COLLECT");
        ClientPlatformServices.getInstance().sendToServer(new CollectPayload(
                screen.getMenu().containerId,
                scopeArray,
                id
        ));
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
            slotMapping[i * 2] = ((ISlot) from).clientsort$getIndexInMenu();
            slotMapping[i * 2 + 1] = ((ISlot) to).clientsort$getIndexInMenu();
        }
        screenHelper.translateSlotIds(slotMapping);
        return slotMapping;
    }

    private void setOverlayMessage(Component message) {
        ClientSort.setOverlayMessage(
                screen,
                localized("name").withStyle(ChatFormatting.RED)
                        .append("\n")
                        .append(message)
                        .append("\n")
                        .append(localized("message", "checkLogs")),
                100
        );
    }
}
