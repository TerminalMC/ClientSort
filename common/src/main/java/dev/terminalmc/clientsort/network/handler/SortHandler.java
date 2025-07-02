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

import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.TreeMap;

import static dev.terminalmc.clientsort.network.handler.util.SlotValidation.validateSlotMapping;

/**
 * A handler for a {@link SortPayload}.
 */
public class SortHandler extends PayloadHandler {

    private SortHandler() {
    }

    public static void handle(
            SortPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        // Execute on main server thread
        server.execute(() -> processPayload(
                server,
                player,
                payload.containerId(),
                (menu) -> validateSlotMapping(player, menu, payload.slotMapping()),
                (menu) -> sort(menu, payload.slotMapping()),
                SortPayload.ID,
                SortResultPayload.ID,
                (error) -> new SortResultPayload(error == null, error == null ? "" : error)
        ));
    }

    private static void sort(AbstractContainerMenu menu, int[] slotMapping) {
        // Build reference maps
        Map<Integer, ItemStack> stacks = new TreeMap<>();
        for (Slot slot : menu.slots) {
            stacks.put(slot.index, slot.getItem());
        }
        // Apply slot mapping
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            int srcSlotId = slotMapping[i];
            int dstSlotId = slotMapping[i + 1];
            if (srcSlotId != dstSlotId) {
                menu.slots.get(dstSlotId).setByPlayer(stacks.get(srcSlotId));
            }
        }
    }
}
