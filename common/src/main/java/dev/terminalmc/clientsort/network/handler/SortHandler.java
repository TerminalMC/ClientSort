/*
 * Copyright 2022 Siphalor
 * Copyright 2026 TerminalMC
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
import dev.terminalmc.clientsort.exception.PayloadHandlerException.InconsistentStateException;
import dev.terminalmc.clientsort.exception.PayloadHandlerException.UnsupportedOpException;
import dev.terminalmc.clientsort.network.handler.validate.PolicyManager;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.TreeMap;

import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.network.handler.validate.SchemaValidator.validateSlotMapping;

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
                (menu) -> checkPolicy(player, menu, payload.slotMapping()),
                (menu) -> validateSlotMapping(player, menu, payload.slotMapping()),
                (menu) -> sort(server, menu, payload.slotMapping()),
                SortPayload.TYPE,
                SortResultPayload.TYPE,
                (result, message) -> new SortResultPayload(result.code, message)
        ));
    }

    private static void sort(MinecraftServer server, AbstractContainerMenu menu, int[] slotMapping)
            throws PayloadHandlerException {
        // Build reference map
        Map<Integer, ItemStack> stacks = new TreeMap<>();
        for (Slot slot : menu.slots)
            stacks.put(((ISlot) slot).clientsort$getIndexInMenu(), slot.getItem().copy());
        // Apply slot mapping
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            int srcSlotId = slotMapping[i];
            int dstSlotId = slotMapping[i + 1];
            Slot dstSlot = menu.slots.get(dstSlotId);
            if (srcSlotId != dstSlotId) {
                // Perform the mapping set
                dstSlot.setByPlayer(stacks.get(srcSlotId));

                // Check that the operation succeeded
                try {
                    int finalSrcSlotId = srcSlotId;
                    int finalDstSlotId = dstSlotId;
                    validate(
                            server,
                            stacks.get(srcSlotId),
                            dstSlot.getItem(),
                            () -> String.format(
                                    "Sort operation failed at slot mapping %d->%d",
                                    finalSrcSlotId,
                                    finalDstSlotId
                            ),
                            (msg) -> setPolicy(menu, slotMapping, msg)
                    );
                } catch (InconsistentStateException e) {
                    // Attempt to revert changes
                    for (int j = 0; j <= i; j += 2) {
                        srcSlotId = slotMapping[j];
                        menu.slots.get(srcSlotId).set(stacks.get(srcSlotId));
                        dstSlotId = slotMapping[j + 1];
                        menu.slots.get(dstSlotId).set(stacks.get(dstSlotId));
                    }
                    throw e;
                }
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
                SortPayload.ID.toString(),
                message
        );
    }
}
