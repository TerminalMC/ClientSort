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

import dev.terminalmc.clientsort.exception.PayloadHandlerException;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Provides validation methods for slot collections included in C2S payloads.
 */
public class SlotValidation {

    /**
     * @throws PayloadHandlerException if the slot ID is not valid in the menu.
     */
    public static void validateSlotId(AbstractContainerMenu menu, int slotId)
            throws PayloadHandlerException {
        // Check that the slot ID is in range
        if (slotId < 0 || slotId >= menu.slots.size()) {
            throw new PayloadHandlerException(String.format(
                    "Payload contains invalid slot ID %d out of range for menu with size %d!",
                    slotId,
                    menu.slots.size()
            ));
        }

        // Check that the slot's index field matches its ID in the menu
        if (menu.slots.get(slotId).index != slotId) {
            throw new PayloadHandlerException(String.format(
                    "Payload contains invalid slot ID %d which does not match the index of that slot (%d)!",
                    slotId,
                    menu.slots.get(slotId).index
            ));
        }
    }

    /**
     * @throws PayloadHandlerException if the slot ID does not refer to a slot in the container.
     *                                 Includes a check of {@link SlotValidation#validateSlotId}.
     */
    public static void validateContainerSlot(
            AbstractContainerMenu menu,
            int slotId,
            Container container
    ) throws PayloadHandlerException {
        // Check that the slot ID is valid
        validateSlotId(menu, slotId);

        // Check that the slot's container is the same as the provided one
        Slot slot = menu.slots.get(slotId);
        if (container != slot.container) {
            throw new PayloadHandlerException(String.format(
                    "Payload contains slots from different inventories, first: '%s', now: '%s'!",
                    container,
                    slot.container
            ));
        }
    }

    /**
     * @throws PayloadHandlerException if the list of slot IDs does not represent a sub-array of the
     *                                 slots in menu.
     */
    public static void validateSlotArray(Player player, AbstractContainerMenu menu, int[] slotIds)
            throws PayloadHandlerException {
        // Check the length of the slot ID array
        int minSlots = 2;
        if (slotIds.length < minSlots) {
            throw new PayloadHandlerException(String.format(
                    "Slot array contains too few slots! Expected at least %d, got %d!",
                    minSlots,
                    slotIds.length
            ));
        }

        // Check that the first slot ID is valid
        validateSlotId(menu, slotIds[0]);

        // Get the first slot's container
        Container container = menu.slots.get(slotIds[0]).container;
        IntSet checkedSlots = new IntAVLTreeSet();

        // For each slot
        for (int slotId : slotIds) {
            // Check that the slot is valid and in the same container
            validateContainerSlot(menu, slotId, container);

            // Check that the slot has not been seen before
            if (!checkedSlots.add(slotId)) {
                throw new PayloadHandlerException(String.format(
                        "Slot array contains duplicate slot %d!",
                        slotId
                ));
            }

            // Check that the slot is accessible
            Slot slot = menu.slots.get(slotId);
            if (slot.hasItem() && !slot.mayPickup(player)) {
                throw new PayloadHandlerException(String.format(
                        "Slot array contains inaccessible slot %d with item '%s'!",
                        slotId,
                        slot.getItem()
                ));
            }
        }
    }

    /**
     * @throws PayloadHandlerException if the slot mapping does not represent a valid reordering for
     *                                 the slots in the menu.
     */
    public static void validateSlotMapping(
            Player player,
            AbstractContainerMenu menu,
            int[] slotMapping
    ) throws PayloadHandlerException {
        // Check the length of the slot mapping array
        int minSlots = 4;
        if (slotMapping.length < minSlots) {
            throw new PayloadHandlerException(String.format(
                    "Slot mapping contains too few slots! Expected at least %d, got %d!",
                    minSlots,
                    slotMapping.length
            ));
        }
        if (slotMapping.length % 2 != 0) {
            throw new PayloadHandlerException(String.format(
                    "Slot mapping contains an uneven number of slots (%d)!",
                    slotMapping.length
            ));
        }

        // Check that the first slot ID is valid
        validateSlotId(menu, slotMapping[0]);

        // Get the first slot's container
        Container container = menu.slots.get(slotMapping[0]).container;
        IntSet checkedSlots = new IntAVLTreeSet();

        // For each pair of slot IDs
        for (int i = 0; i < slotMapping.length; i += 2) {
            int srcId = slotMapping[i];
            int dstId = slotMapping[i + 1];

            // Check that the source slot is valid and in the same container
            validateContainerSlot(menu, srcId, container);

            // Check that the source slot has not been seen before
            if (!checkedSlots.add(srcId)) {
                throw new PayloadHandlerException(String.format(
                        "Slot mapping contains duplicate source slot %d!",
                        srcId
                ));
            }

            // Check that the destination slot is valid and in the same
            // container
            validateContainerSlot(menu, dstId, container);

            // Transferring items between the same slot is a no-op
            if (srcId == dstId)
                continue;

            // Check that the source slot is accessible
            Slot srcSlot = menu.slots.get(srcId);
            if (srcSlot.hasItem() && !srcSlot.mayPickup(player)) {
                throw new PayloadHandlerException(String.format(
                        "Slot mapping contains inaccessible slot %d with item '%s'!",
                        srcId,
                        srcSlot.getItem()
                ));
            }

            // Check that the destination slot is accessible
            Slot dstSlot = menu.slots.get(dstId);
            if (!dstSlot.mayPlace(srcSlot.getItem())) {
                throw new PayloadHandlerException(String.format(
                        "Slot mapping contains inaccessible slot %d with item '%s' which cannot receive item '%s' from slot %d!",
                        dstId,
                        dstSlot.getItem(),
                        srcSlot.getItem(),
                        srcId
                ));
            }
        }

        // Check that all destination IDs appear as source IDs exactly once
        for (int i = 1; i < slotMapping.length; i += 2) {
            int dstId = slotMapping[i];
            if (!checkedSlots.remove(dstId)) {
                throw new PayloadHandlerException(String.format(
                        "Slot mapping contains duplicate destination slot or destination slot that does not appear as source slot (%d)!",
                        dstId
                ));
            }
        }
        if (!checkedSlots.isEmpty()) {
            throw new PayloadHandlerException(String.format(
                    "Slot mapping contains %d source slots that do not appear as destination slots.",
                    checkedSlots.size()
            ));
        }
    }
}
