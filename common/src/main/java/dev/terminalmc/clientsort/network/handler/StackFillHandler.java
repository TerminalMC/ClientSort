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
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.StackFillResultPayload;
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

public class StackFillHandler {
    private StackFillHandler() {}
    
    public static void handle(
            StackFillPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        // Execute on main server thread
        server.execute(() -> handleStackFillPayload(payload, server, player));
    }
    
    public static void handleStackFillPayload(
            StackFillPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        @Nullable AbstractContainerMenu menu = null;
        @Nullable String error = null;

        try {
            // Check menu
            menu = getMenu(payload.srcId(), player);
            menu.suppressRemoteUpdates();

            Map<Integer, Slot> slots = new TreeMap<>();
            for (Slot slot : menu.slots) {
                slots.put(slot.index, slot);
            }

            validateSlotArray(player, slots, payload.srcSlots());
            validateSlotArray(player, slots, payload.dstSlots());

            // Perform operation

            // Work backwards from end of source slot array, looking for an
            // accessible stack
            for (int i = payload.srcSlots().length - 1; i >= 0; i--) {
                int srcId = payload.srcSlots()[i];
                Slot srcSlot = menu.slots.get(srcId);
                ItemStack srcStack = srcSlot.getItem();

                if (srcStack.isEmpty()) continue;
                if (!srcSlot.mayPickup(player)) continue;

                // Work forwards from start of destination slot array, looking
                // for a matching partial stack
                for (int j = 0; j < payload.dstSlots().length; j++) {
                    int dstId = payload.dstSlots()[j];
                    Slot dstSlot = slots.get(dstId);
                    ItemStack dstStack = dstSlot.getItem();

                    if (dstStack.isEmpty()) continue;
                    if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack)) continue;
                    if (!ItemStack.isSameItemSameComponents(srcStack, dstStack)) continue;
                    
                    dstSlot.safeInsert(srcStack);

                    // If no items remain in the source stack, stop looking
                    if (srcStack.isEmpty()) break;
                    // Otherwise keep looking for another slot
                }
            }

        } catch (Exception e) {
            if (e instanceof ClientSortException se) {
                error = se.getMessage();
            } else {
                error = ClientSortException.GENERIC_MESSAGE;
                ClientSort.LOG.error(
                        "Unexpected exception while handling payload '{}' from player '{}'",
                        StackFillPayload.TYPE_LOCATION, player, e);
            }
        } finally {
            if (menu != null) {
                menu.resumeRemoteUpdates();
                menu.broadcastChanges();
            }
            if (Services.PLATFORM.canSendToPlayer(player, StackFillResultPayload.TYPE)) {
                Services.PLATFORM.sendToPlayer(player,
                        new StackFillResultPayload(error == null, error == null ? "" : error));
            }
        }
    }
}
