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

package dev.terminalmc.clientsort.network.handler.util;

import dev.terminalmc.clientsort.exception.ClientSortException;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import java.util.Map;

// TODO check isValidSlotIndex

public class SlotValidation {
    /**
     * @throws ClientSortException if slotId is not in slots.
     */
    public static void validateSlotId(Map<Integer, Slot> slots, int slotId)
            throws ClientSortException {
        if (!slots.containsKey(slotId)) {
            StringBuilder sb = new StringBuilder();
            slots.keySet().forEach((key) -> sb.append(key).append(", "));
            throw new ClientSortException(String.format(
                    "Sort payload contains invalid slot ID %s not found in list [%s]!",
                    slotId, sb));
        }
    }

    /**
     * @throws ClientSortException if the slotId does not refer to a slot in
     * container.
     */
    public static void validateSlot(Map<Integer,Slot> slots, int slotId, Container container)
            throws ClientSortException {
        validateSlotId(slots, slotId);
        Slot slot = slots.get(slotId);

        if (container != slot.container) {
            throw new ClientSortException(String.format(
                    "Sort payload contains slots from different inventories, first: %s, now: %s!",
                    container, slot.container));
        }
    }

    /**
     * @throws ClientSortException if slotIds is not valid for slots.
     */
    public static void validateSlotArray(Player player, Map<Integer,Slot> slots, int[] slotIds)
            throws ClientSortException {
        int minSlots = 2;
        if (slotIds.length < minSlots) {
            throw new ClientSortException(String.format(
                    "Slot array contains too few slots! Got %d, expected at least %d",
                    slotIds.length, minSlots));
        }

        validateSlotId(slots, slotIds[0]);

        IntSet checkedSlots = new IntAVLTreeSet();
        Container container = slots.get(slotIds[0]).container;

        for (int slotId : slotIds) {
            validateSlot(slots, slotId, container);

            if (!checkedSlots.add(slotId)) {
                throw new ClientSortException(String.format(
                        "Slot array contains duplicate slot %s!",
                        slotId));
            }

            Slot slot = slots.get(slotId);
            if (!slot.mayPickup(player)) {
                throw new ClientSortException(String.format(
                        "Slot array contains inaccessible slot %s with item %s!",
                        slotId, slot.getItem()));
            }
        }
    }

    /**
     * @throws ClientSortException if slotMapping is not valid for slots.
     */
    public static void validateSlotMapping(Player player, Map<Integer,Slot> slots, int[] slotMapping)
            throws ClientSortException {
        int minSlots = 4;
        if (slotMapping.length < minSlots) {
            throw new ClientSortException(String.format(
                    "Slot mapping contains too few slots! Got %d, expected at least %d",
                    slotMapping.length, minSlots));
        }

        if (slotMapping.length % 2 != 0) {
            throw new ClientSortException(String.format(
                    "Slot mapping contains an uneven number of slots! Found %d",
                    slotMapping.length));
        }

        validateSlotId(slots, slotMapping[0]);

        IntSet checkedSlots = new IntAVLTreeSet();
        Container container = slots.get(slotMapping[0]).container;

        for (int i = 0; i < slotMapping.length; i += 2) {
            int srcSlotId = slotMapping[i];
            int dstSlotId = slotMapping[i + 1];

            validateSlot(slots, srcSlotId, container);

            if (!checkedSlots.add(srcSlotId)) {
                throw new ClientSortException(String.format(
                        "Slot mapping contains duplicate source slot %s!",
                        srcSlotId));
            }

            validateSlot(slots, dstSlotId, container);

            if (srcSlotId == dstSlotId) continue;

            Slot srcSlot = slots.get(srcSlotId);
            if (!srcSlot.mayPickup(player)) {
                throw new ClientSortException(String.format(
                        "Slot mapping contains inaccessible slot %s with item %s!",
                        srcSlotId, srcSlot.getItem()));
            }

            Slot dstSlot = slots.get(dstSlotId);
            if (!dstSlot.mayPlace(srcSlot.getItem())) {
                throw new ClientSortException(String.format(
                        "Slot mapping contains inaccessible slot %s with item %s which cannot receive item %s from slot %s!",
                        dstSlotId, dstSlot.getItem(), srcSlot.getItem(), srcSlotId));
            }
        }

        for (int i = 1; i < slotMapping.length; i += 2) {
            int dstSlotId = slotMapping[i];
            if (!checkedSlots.remove(dstSlotId)) {
                throw new ClientSortException(String.format(
                        "Slot mapping contains duplicate destination slot or slot without source %s!",
                        dstSlotId));
            }
        }
        if (!checkedSlots.isEmpty()) {
            throw new ClientSortException(String.format(
                    "Slot mapping is invalid: checkedSlots not empty, %d remaining.",
                    checkedSlots.size()));
        }
    }
}
