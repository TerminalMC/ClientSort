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

import dev.terminalmc.clientsort.config.ClassPolicy;
import dev.terminalmc.clientsort.exception.PayloadHandlerException;
import dev.terminalmc.clientsort.network.handler.validate.PolicyManager;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.TreeMap;

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
                (menu) -> sort(menu, payload.slotMapping()),
                SortResultPayload.TYPE,
                (error) -> new SortResultPayload(error == null, error == null ? "" : error)
        ));
    }

    private static void sort(AbstractContainerMenu menu, int[] slotMapping)
            throws PayloadHandlerException {
        // Build reference map
        Map<Integer, ItemStack> stacks = new TreeMap<>();
        for (Slot slot : menu.slots)
            stacks.put(((ISlot) slot).clientsort$getIdInContainer(), slot.getItem().copy());
        // Apply slot mapping
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            int srcSlotId = slotMapping[i];
            int dstSlotId = slotMapping[i + 1];
            Slot dstSlot = menu.slots.get(dstSlotId);
            if (srcSlotId != dstSlotId) {
                // Perform the mapping set
                dstSlot.setByPlayer(stacks.get(srcSlotId));

                // Check that the operation succeeded
                if (notEqual(dstSlot.getItem(), stacks.get(srcSlotId))) {
                    // Operation failed; attempt to revert all changes
                    for (int j = 0; j <= i; j += 2) {
                        srcSlotId = slotMapping[j];
                        menu.slots.get(srcSlotId).set(stacks.get(srcSlotId));
                        dstSlotId = slotMapping[j + 1];
                        menu.slots.get(dstSlotId).set(stacks.get(dstSlotId));
                    }
                    setPolicy(menu, slotMapping);
                    throw new PayloadHandlerException(String.format(
                            "Sort operation failed at slot mapping %d->%d: Expected '%s' in destination after set, got '%s'!",
                            srcSlotId,
                            dstSlotId,
                            stacks.get(srcSlotId),
                            dstSlot.getItem()
                    ));
                }
            }
        }
    }

    /**
     * @throws PayloadHandlerException if there is a policy for this context disallowing this
     *                                 operation.
     */
    private static void checkPolicy(ServerPlayer player, AbstractContainerMenu menu, int[] slotIds)
            throws PayloadHandlerException {
        Container container = slotIds.length > 0
                ? menu.slots.get(slotIds[0]).container
                : null;
        Object object = container instanceof SimpleContainer ? menu : container;

        // Assume the player's own inventory is always safe to operate on
        if (container != player.getInventory()) {
            PolicyManager.checkPolicy(object.getClass(), (bl) -> bl.sort);
        }
    }

    /**
     * Creates or updates a policy for this context to disallow this operation.
     */
    private static void setPolicy(AbstractContainerMenu menu, int[] slotIds) {
        Container container = slotIds.length > 0
                ? menu.slots.get(slotIds[0]).container
                : null;
        Object object = container instanceof SimpleContainer ? menu : container;

        PolicyManager.setPolicy(new ClassPolicy(object.getClass().getName(), true, false, true));
    }
}
