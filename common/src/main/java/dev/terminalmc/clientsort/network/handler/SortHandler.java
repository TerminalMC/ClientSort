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
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
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
import static dev.terminalmc.clientsort.network.handler.util.SlotValidation.validateSlotMapping;

public class SortHandler {
    private SortHandler() {}
    
    public static void onSortPayload(SortPayload payload, MinecraftServer server,
                                     ServerPlayer player) {
        // Execute on main server thread
        server.execute(() -> handleSortPayload(payload, server, player));
    }
    
    private static void handleSortPayload(SortPayload payload, MinecraftServer server,
                                          ServerPlayer player) {
        @Nullable AbstractContainerMenu menu = null;
        @Nullable String error = null;

        try {
            menu = getMenu(payload.syncId(), player);
            menu.suppressRemoteUpdates();

            // Build slot map
            Map<Integer, Slot> inventorySlots = new TreeMap<>();
            Map<Integer, ItemStack> inventoryStacks = new TreeMap<>();
            for (Slot slot : menu.slots) {
                inventorySlots.put(slot.index, slot);
                inventoryStacks.put(slot.index, slot.getItem());
            }

            // Validate packet slots
            validateSlotMapping(player, inventorySlots, payload.slotMapping());

            // Combine all partial stacks
            sortSlots(inventorySlots, inventoryStacks, payload.slotMapping());

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
            if (Services.PLATFORM.canSendToPlayer(player, SortResultPayload.TYPE)) {
                Services.PLATFORM.sendToPlayer(player, new SortResultPayload(
                        error == null, error == null ? "" : error));
            }
        }
    }

    private static void sortSlots(Map<Integer,Slot> inventorySlots,
                                  Map<Integer,ItemStack> inventoryStacks, int[] slotMapping) {
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            int srcSlotId = slotMapping[i];
            int dstSlotId = slotMapping[i + 1];
            inventorySlots.get(dstSlotId).setByPlayer(inventoryStacks.get(srcSlotId));
        }
    }


//    /**
//     * Serverside sorting method for reference only.
//     */
//    private static void sort(ServerPlayer player, AbstractContainerMenu menu,
//                             int[] slotIds, SortOrder sortOrder) {
//        menu.suppressRemoteUpdates();
//
//        // Build slot map
//        Map<Integer,Slot> inventorySlots = new TreeMap<>();
//        for (Slot slot : menu.slots) {
//            inventorySlots.put(slot.index, slot);
//        }
//
//        // Combine all partial stacks
//        collectSlots(inventorySlots, slotIds);
//
//        // Build stack map
//        Map<Integer,ItemStack> inventoryStacks = new TreeMap<>();
//        for (Slot slot : menu.slots) {
//            inventoryStacks.put(slot.index, slot.getItem());
//        }
//
//        // Create arrays of slot numbers and stacks for sorting
//        ItemStack[] stacks = new ItemStack[slotIds.length];
//        int[] sortedSlots = new int[slotIds.length];
//        for (int i = 0; i < slotIds.length; i++) {
//            stacks[i] = inventorySlots.get(slotIds[i]).getItem();
//            sortedSlots[i] = i;
//        }
//
//        // Sort the array of slot numbers to make an array of indexes indicating
//        // how packetSlots should be reordered
//        sortedSlots = sortOrder.sort(sortedSlots, stacks, new SortContext(player.level()));
//
//        // Perform sort
//        for (int i = 0; i < sortedSlots.length; i++) {
//            inventorySlots.get(slotIds[i]).setByPlayer(inventoryStacks.get(slotIds[sortedSlots[i]]));
//        }
//
//        menu.resumeRemoteUpdates();
//        menu.broadcastChanges();
//    }
}
