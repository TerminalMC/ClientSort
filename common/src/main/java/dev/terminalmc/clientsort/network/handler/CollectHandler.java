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

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.config.ServerClassPolicy;
import dev.terminalmc.clientsort.exception.PayloadHandlerException;
import dev.terminalmc.clientsort.exception.PayloadHandlerException.UnsupportedOpException;
import dev.terminalmc.clientsort.network.handler.validate.PolicyManager;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.CollectResultPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.network.handler.validate.SchemaValidator.validateSlotArray;

/**
 * A handler for a {@link CollectPayload}.
 */
public class CollectHandler extends PayloadHandler {

    private CollectHandler() {
    }

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
                (menu) -> checkPolicy(player, menu, payload.slotIds()),
                (menu) -> validateSlotArray(player, menu, payload.slotIds()),
                (menu) -> collect(server, menu, payload.slotIds()),
                CollectPayload.TYPE,
                CollectResultPayload.TYPE,
                (result, message) -> new CollectResultPayload(result.code, message)
        ));
    }

    private static void collect(MinecraftServer server, AbstractContainerMenu menu, int[] slotIds)
            throws PayloadHandlerException {
        // Work backwards from the end, looking for a partial stack
        for (int i = slotIds.length - 1; i >= 0; i--) {
            int srcSlotId = slotIds[i];
            Slot srcSlot = menu.slots.get(srcSlotId);
            ItemStack srcStack = srcSlot.getItem();
            ItemStack srcStackCopy = srcStack.copy();

            if (srcStack.isEmpty())
                continue;
            if (srcStack.getCount() >= srcStack.getItem().getDefaultMaxStackSize())
                continue;

            // Partial stack found; work forwards from the start, looking for
            // another partial stack of the same item
            for (int j = 0; j < i; j++) {
                int dstSlotId = slotIds[j];
                Slot dstSlot = menu.slots.get(dstSlotId);
                ItemStack dstStack = dstSlot.getItem();
                ItemStack dstStackCopy = dstStack.copy();

                if (dstStack.isEmpty())
                    continue;
                if (dstStack.getCount() >= dstStack.getItem().getDefaultMaxStackSize())
                    continue;
                if (!ItemStack.isSameItemSameComponents(srcStack, dstStack))
                    continue;

                // Matching partial stack found; place as much of the source
                // stack as possible
                dstSlot.safeInsert(srcStack);

                // Check that the operation succeeded
                ItemStack expected = srcStackCopy.copyWithCount(Math.min(
                        srcStackCopy.getCount() + dstStackCopy.getCount(),
                        dstSlot.getMaxStackSize(srcStackCopy)
                ));
                validate(
                        server,
                        expected,
                        dstSlot.getItem(),
                        () -> String.format(
                                "Collect operation failed to safe-insert from slot %d with item '%s' to slot %d with item '%s'",
                                srcSlotId,
                                srcStackCopy,
                                dstSlotId,
                                dstStackCopy
                        ),
                        (msg) -> setPolicy(menu, slotIds, msg)
                );

                // If no items remain in the source stack, stop looking
                if (srcStack.isEmpty())
                    break;
                // Otherwise keep looking for another matching partial stack
            }
        }
    }

    /**
     * @throws UnsupportedOpException if there is a policy for this context disallowing this
     *                                operation.
     */
    private static void checkPolicy(ServerPlayer player, AbstractContainerMenu menu, int[] slotIds)
            throws UnsupportedOpException {
        Container container = slotIds.length > 0
                ? menu.slots.get(slotIds[0]).container
                : null;
        Object object = getObj(container, menu);
        if (object == null)
            throw new UnsupportedOpException("Reference object is null for inputs '%s', '%s'!".formatted(
                    container == null ? "null" : container.getClass().getName(),
                    menu == null ? "null" : menu.getClass().getName()
            ));

        // Assume the player's own inventory is always safe to operate on
        if (container != player.getInventory()) {
            PolicyManager.checkPolicy(object.getClass(), (bl) -> bl.sortEnabled);
        }
    }

    /**
     * Creates or updates a policy for this context to disallow this operation.
     */
    private static void setPolicy(AbstractContainerMenu menu, int[] slotIds, String message) {
        Container container = slotIds.length > 0
                ? menu.slots.get(slotIds[0]).container
                : null;
        Object object = getObj(container, menu);
        if (object == null) {
            ClientSort.LOG.warn(
                    "Could not set policy: reference object is null for inputs '{}', '{}'!",
                    container == null ? "null" : container.getClass().getName(),
                    menu == null ? "null" : menu.getClass().getName()
            );
            return;
        }

        PolicyManager.setPolicy(
                new ServerClassPolicy(
                        object.getClass().getName(),
                        false,
                        true,
                        true
                ),
                CollectPayload.ID.toString(),
                message
        );
    }
}
