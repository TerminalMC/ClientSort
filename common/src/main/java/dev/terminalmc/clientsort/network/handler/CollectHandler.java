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

package dev.terminalmc.clientsort.network.handler;

import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.CollectResultPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static dev.terminalmc.clientsort.network.handler.util.SlotValidation.validateSlotArray;

/**
 * A handler for a {@link CollectPayload}.
 */
public class CollectHandler extends PayloadHandler {
    private CollectHandler() {}

    public static void handle(
            CollectPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        // Execute on main server thread
        server.execute(() -> processPayload(
                server,
                player,
                payload.containerId(),
                (menu) -> validateSlotArray(player, menu, payload.slotIds()),
                (menu) -> collect(menu, payload.slotIds()),
                CollectPayload.ID,
                CollectResultPayload.ID,
                (error) -> new CollectResultPayload(error == null, error == null ? "" : error)
        ));
    }

    private static void collect(AbstractContainerMenu menu, int[] slotIds) {
        // Work backwards from the end, looking for a partial stack
        for (int i = slotIds.length - 1; i >= 0; i--) {
            Slot srcSlot = menu.slots.get(slotIds[i]);
            ItemStack srcStack = srcSlot.getItem();

            if (srcStack.isEmpty()) continue;
            if (srcStack.getCount() >= srcStack.getItem().getMaxStackSize()) continue;

            // Partial stack found; work forwards from the start, looking for
            // another partial stack of the same item
            for (int j = 0; j < i; j++) {
                Slot dstSlot = menu.slots.get(slotIds[j]);
                ItemStack dstStack = dstSlot.getItem();

                if (dstStack.isEmpty()) continue;
                if (dstStack.getCount() >= dstStack.getItem().getMaxStackSize()) continue;
                if (!ItemStack.isSameItemSameTags(srcStack, dstStack)) continue;

                // Matching partial stack found; place as much of the source
                // stack as possible
                dstSlot.safeInsert(srcStack);

                // If no items remain in the source stack, stop looking
                if (srcStack.isEmpty()) break;
                // Otherwise keep looking for another matching partial stack
            }
        }
    }
}
