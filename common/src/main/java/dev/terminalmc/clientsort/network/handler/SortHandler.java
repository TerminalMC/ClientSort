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

import dev.terminalmc.clientsort.exception.PayloadHandlerException;
import dev.terminalmc.clientsort.exception.PayloadHandlerException.InconsistentStateException;
import dev.terminalmc.clientsort.exception.PayloadHandlerException.UnsupportedOpException;
import dev.terminalmc.clientsort.network.handler.validate.PolicyManager;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

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

    private static void sort(
            MinecraftServer server,
            AbstractContainerMenu menu,
            int[] slotMapping
    ) throws PayloadHandlerException {
        // build a reverse-lookup map
        Int2IntMap map = new Int2IntOpenHashMap();
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            map.put(slotMapping[i + 1], slotMapping[i]);
        }

        // create a temporary slot to hold the overflow
        Container tmpContainer = new SimpleContainer(1);
        Slot tmpSlot = new Slot(tmpContainer, 0, 0, 0);
        List<Slot> undoChain = new ArrayList<>();

        // iterate over the whole mapping to ensure we cover all isolated chains
        for (int i = 1; i < slotMapping.length; i += 2) {
            undoChain.clear();
            int startSlotId = slotMapping[i];

            // have we already seen this slot as part of a previous chain?
            if (!map.containsKey(startSlotId))
                continue;

            // move the starting slot's item into temporary storage so it doesn't get lost
            Slot startSlot = menu.getSlot(startSlotId);
            tmpSlot.setByPlayer(startSlot.getItem());

            // set up the starting condition
            Slot dstSlot;
            int srcSlotId = slotMapping[i - 1];
            Slot srcSlot = startSlot;

            // iterate backwards along the chain until we reach the end,
            // signified by seeing an ID that we've previously removed
            while (map.containsKey(map.getOrDefault(srcSlotId, Integer.MIN_VALUE))) {
                // new dest is old source
                dstSlot = srcSlot;
                // new source is old source's source
                srcSlot = menu.slots.get(srcSlotId);
                srcSlotId = map.remove(srcSlotId);

                // At this point, though we should have already validated it, we need to
                // confirm that it's valid to place the source item in the dest slot.
                // If it isn't, we can't really recover without iterating backwards over
                // the whole array to validate every move and even that isn't guaranteed
                // to work. So instead we just panic.
                if (!dstSlot.mayPlace(srcSlot.getItem())) {
                    // don't have any good options for recovery, so just try to reverse it
                    Slot recSrcSlot;
                    Slot recDstSlot;

                    for (int j = undoChain.size() - 1; j > 0; j--) {
                        recSrcSlot = undoChain.get(j);
                        recDstSlot = undoChain.get(j - 1);

                        recSrcSlot.setByPlayer(recDstSlot.getItem());
                    }

                    startSlot.setByPlayer(tmpSlot.getItem());

                    throw new InconsistentStateException(String.format(
                            "Item %s from slot %d cannot be placed in slot %d. Has the inventory changed?",
                            srcSlot.getItem().getDisplayName().getString(),
                            srcSlot.index,
                            dstSlot.index
                    ));
                }

                // move the source slot's item into the dest slot
                dstSlot.setByPlayer(srcSlot.getItem());

                undoChain.add(dstSlot);
                undoChain.add(srcSlot);
            }

            // end of the chain; srcSlot is now the dest of startSlot,
            // so retrieve startSlot's item from temp storage
            srcSlot.setByPlayer(tmpSlot.getItem());
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
}
