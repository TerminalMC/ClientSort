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

import dev.terminalmc.clientsort.network.payload.TransferPayload;
import dev.terminalmc.clientsort.network.payload.TransferResultPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static dev.terminalmc.clientsort.network.handler.util.SlotValidation.validateSlotArray;

/**
 * A handler for a {@link TransferPayload}.
 */
public class TransferHandler extends PayloadHandler {

    private TransferHandler() {
    }

    public static void handle(
            TransferPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        // Execute on main server thread
        server.execute(() -> processPayload(
                server,
                player,
                payload.srcContainerId(),
                (menu) -> {
                    validateSlotArray(player, menu, payload.srcSlotIds());
                    validateSlotArray(player, menu, payload.dstSlotIds());
                },
                (menu) -> transfer(menu, payload.srcSlotIds(), payload.dstSlotIds()),
                TransferResultPayload.TYPE,
                (error) -> new TransferResultPayload(error == null, error == null ? "" : error)
        ));
    }

    private static void transfer(AbstractContainerMenu menu, int[] srcSlotIds, int[] dstSlotIds) {
        // Work backwards from the end of the source array, looking for a
        // nonempty stack
        for (int i = srcSlotIds.length - 1; i >= 0; i--) {
            Slot srcSlot = menu.slots.get(srcSlotIds[i]);
            ItemStack srcStack = srcSlot.getItem();

            if (srcStack.isEmpty())
                continue;

            // Nonempty stack found; work forwards from the start of the
            // destination array, looking for a partial stack of the same item
            for (int slotId : dstSlotIds) {
                Slot dstSlot = menu.slots.get(slotId);
                ItemStack dstStack = dstSlot.getItem();

                if (dstStack.isEmpty())
                    continue;
                if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack))
                    continue;
                if (!ItemStack.isSameItemSameComponents(srcStack, dstStack))
                    continue;

                // Matching partial stack found; place as much of the source
                // stack as possible
                dstSlot.safeInsert(srcStack);

                // If no items remain in the source stack, stop looking
                if (srcStack.isEmpty())
                    break;
                // Otherwise keep looking for another matching partial stack
            }

            if (srcStack.isEmpty())
                continue;

            // Source stack is still not empty; work forwards from the start of
            // the destination array, looking for an empty slot
            for (int dstSlotId : dstSlotIds) {
                Slot dstSlot = menu.slots.get(dstSlotId);
                ItemStack dstStack = dstSlot.getItem();

                if (!dstStack.isEmpty())
                    continue;

                // Empty slot found; place the source stack
                dstSlot.safeInsert(srcStack);
                break;
            }
        }
    }
}
