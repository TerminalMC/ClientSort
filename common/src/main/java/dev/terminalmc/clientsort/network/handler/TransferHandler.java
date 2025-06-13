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
    private TransferHandler() {}

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
                (menu) -> validateSlotArray(player, menu, payload.srcSlotIds()),
                (menu) -> transfer(player, menu, payload.srcSlotIds()),
                TransferResultPayload.TYPE,
                (error) -> new TransferResultPayload(error == null, error == null ? "" : error)
        ));
    }

    private static void transfer(
            ServerPlayer player,
            AbstractContainerMenu menu,
            int[] srcSlotIds
    ) {
        // Work backwards from the end of the source slot array
        for (int i = srcSlotIds.length - 1; i >= 0; i--) {
            int srcSlotId = srcSlotIds[i];
            Slot srcSlot = menu.slots.get(srcSlotId);

            ItemStack srcStack = menu.quickMoveStack(player, srcSlotId);

            // Quick-move the slot
            while (!srcStack.isEmpty() && ItemStack.isSameItem(srcSlot.getItem(), srcStack)) {
                srcStack = menu.quickMoveStack(player, srcSlotId);
            }
        }
    }
}
