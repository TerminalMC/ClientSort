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

package dev.terminalmc.clientsort.network.handler;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.exception.ClientSortException;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.CollectResultPayload;
import dev.terminalmc.clientsort.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

import static dev.terminalmc.clientsort.network.handler.util.HandlerUtil.getMenu;
import static dev.terminalmc.clientsort.network.handler.util.SlotValidation.validateSlotArray;

public class CollectHandler {
    private CollectHandler() {}
    
    public static void handle(
            CollectPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        // Execute on main server thread
        server.execute(() -> handleCollectPayload(payload, server, player));
    }
    
    public static void handleCollectPayload(
            CollectPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        @Nullable AbstractContainerMenu menu = null;
        @Nullable String error = null;

        try {
            menu = getMenu(payload.syncId(), player);
            menu.suppressRemoteUpdates();

            // Build slot map
            Map<Integer, Slot> inventorySlots = new TreeMap<>();
            for (Slot slot : menu.slots) {
                inventorySlots.put(slot.index, slot);
            }

            // Validate packet slots
            validateSlotArray(player, inventorySlots, payload.slots());

            // Combine all partial stacks
            collectSlots(inventorySlots, payload.slots());

        } catch (Exception e) {
            if (e instanceof ClientSortException se) {
                error = se.getMessage();
            } else {
                error = ClientSortException.GENERIC_MESSAGE;
                ClientSort.LOG.error(
                        "Unexpected exception while handling collect payload from player '{}'",
                        player, e);
            }
        } finally {
            if (menu != null) {
                menu.resumeRemoteUpdates();
                menu.broadcastChanges();
            }
            if (Services.PLATFORM.canSendToPlayer(player, CollectResultPayload.TYPE)) {
                Services.PLATFORM.sendToPlayer(player,
                        new CollectResultPayload(error == null, error == null ? "" : error));
            }
        }
    }

    private static void collectSlots(Map<Integer,Slot> inventorySlots, int[] slotIds) {
        // Work backwards from the end, looking for a partial stack
        for (int i = slotIds.length - 1; i >= 0; i--) {
            Slot originSlot = inventorySlots.get(slotIds[i]);
            ItemStack originStack = originSlot.getItem();

            if (originStack.isEmpty()) continue;
            if (originStack.getCount() >= originStack.getItem().getDefaultMaxStackSize()) continue;

            // Partial stack found; work forwards from the start, looking for
            // another partial stack of the same item
            for (int j = 0; j < i; j++) {
                Slot targetSlot = inventorySlots.get(slotIds[j]);
                ItemStack targetStack = targetSlot.getItem();

                if (targetStack.isEmpty()) continue;
                if (targetStack.getCount() >= targetStack.getItem().getDefaultMaxStackSize()) continue;

                if (ItemStack.isSameItemSameComponents(originStack, targetStack)) {
                    // Matching partial stack found, place as much of the origin
                    // stack as possible
                    targetSlot.safeInsert(originStack);

                    // If no items remain in the carried stack, stop looking
                    if (originStack.isEmpty()) break;
                    // Otherwise keep looking for another matching stack
                }
            }
        }
    }
}
