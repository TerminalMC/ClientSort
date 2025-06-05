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
import dev.terminalmc.clientsort.network.payload.TransferPayload;
import dev.terminalmc.clientsort.network.payload.TransferResultPayload;
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

public class TransferHandler {
    private TransferHandler() {}
    
    public static void handle(
            TransferPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        // Execute on main server thread
        server.execute(() -> handleTransferPayload(payload, server, player));
    }
    
    public static void handleTransferPayload(
            TransferPayload payload,
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

            // TODO dstSlots not actually used, remove it?
            //  if not removing, need to check for overlap
            
            // Perform operation
            
            // Work backwards from end of source slot array, looking for an
            // accessible stack
            for (int i = payload.srcSlots().length - 1; i >= 0; i--) {
                int srcId = payload.srcSlots()[i];
                Slot srcSlot = menu.slots.get(srcId);
                
                if (!srcSlot.mayPickup(player)) continue;
                
                ItemStack srcStack = menu.quickMoveStack(player, srcId);

                while (!srcStack.isEmpty() && ItemStack.isSameItem(srcSlot.getItem(), srcStack)) {
                    srcStack = menu.quickMoveStack(player, srcId);
                }
            }

        } catch (Exception e) {
            if (e instanceof ClientSortException se) {
                error = se.getMessage();
            } else {
                error = ClientSortException.GENERIC_MESSAGE;
                ClientSort.LOG.error(
                        "Unexpected exception while handling payload '{}' from player '{}'",
                        TransferPayload.TYPE_LOCATION, player, e);
            }
        } finally {
            if (menu != null) {
                menu.resumeRemoteUpdates();
                menu.broadcastChanges();
            }
            if (Services.PLATFORM.canSendToPlayer(player, TransferResultPayload.TYPE)) {
                Services.PLATFORM.sendToPlayer(player,
                        new TransferResultPayload(error == null, error == null ? "" : error));
            }
        }
    }
}
