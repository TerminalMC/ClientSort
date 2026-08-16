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

package dev.terminalmc.clientsort.network.handler.validate;

import dev.terminalmc.clientsort.exception.PayloadHandlerException.InvalidDataException;
import dev.terminalmc.clientsort.util.inject.ISlot;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Provides validation methods for slot collections included in C2S payloads.
 */
public class SchemaValidator {

    private SchemaValidator() {
    }

    /**
     * @throws InvalidDataException if the slot ID is not valid in the menu.
     */
    public static void validateSlotId(AbstractContainerMenu menu, int slotId)
            throws InvalidDataException {
        // Check that the slot ID is in range
        if (slotId < 0 || slotId >= menu.slots.size()) {
            throw new InvalidDataException(String.format(
                    "Payload contains invalid slot ID %d out of range for menu with size %d!",
                    slotId,
                    menu.slots.size()
            ));
        }

        // Do a reverse lookup for the slot ID to check that it matches
        int realId = ((ISlot) menu.slots.get(slotId)).clientsort$getIndexInMenu();
        if (slotId != realId) {
            throw new InvalidDataException(String.format(
                    "Payload contains invalid slot ID %d which does not match the known ID of that slot (%d)!",
                    slotId,
                    realId
            ));
        }
    }

    /**
     * @throws InvalidDataException if the slot ID does not refer to a slot in the container.
     *                              Includes a check of {@link SchemaValidator#validateSlotId}.
     */
    public static void validateContainerSlot(
            AbstractContainerMenu menu,
            int slotId,
            Container container
    ) throws InvalidDataException {
        // Check that the slot ID is valid
        validateSlotId(menu, slotId);

        // Check that the slot's container is the same as the provided one
        Slot slot = menu.slots.get(slotId);
        if (container != slot.container) {
            //noinspection ConstantValue
            throw new InvalidDataException(String.format(
                    "Payload contains slots from different containers, first: '%s', now: '%s'!",
                    container == null ? "null" : container.getClass().getName(),
                    slot.container == null ? "null" : slot.container.getClass().getName()
            ));
        }
    }

    /**
     * @throws InvalidDataException if the list of slot IDs does not represent a sub-array of the
     *                              slots in menu.
     */
    public static void validateSlotArray(
            ServerPlayer player,
            AbstractContainerMenu menu,
            int[] slotIds
    ) throws InvalidDataException {
        // Check the length of the slot ID array
        int minSlots = 1;
        if (slotIds.length < minSlots) {
            throw new InvalidDataException(String.format(
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

        ItemStack testItem = Items.LIGHT.getDefaultInstance();
        // For each slot
        for (int slotId : slotIds) {
            // Check that the slot is valid and in the same container
            validateContainerSlot(menu, slotId, container);

            // Check that the slot has not been seen before
            if (!checkedSlots.add(slotId)) {
                throw new InvalidDataException(String.format(
                        "Slot array contains duplicate slot %d!",
                        slotId
                ));
            }

            // Check that the slot is accessible
            Slot slot = menu.slots.get(slotId);
            boolean accessible = true;
            if (slot.hasItem()) {
                // Nonempty slot; check pickup
                if (!slot.mayPickup(player))
                    accessible = false;
            } else {
                // Empty slot; check arbitrary item placement
                if (!slot.container.canPlaceItem(slot.getContainerSlot(), testItem) || !slot.mayPlace(testItem))
                    accessible = false;
            }
            if (!accessible) {
                throw new InvalidDataException(String.format(
                        "Slot array contains inaccessible slot %d with item '%s'!",
                        slotId,
                        slot.getItem()
                ));
            }
        }
    }

    /**
     * For a slot mapping list to be valid, it must meet the following conditions:
     * <ol>
     *     <li>The list must be of even length, and at least 2.
     *     <li>Every slot must be from the same container as the first slot.
     *     <li>Every slot must be valid in the menu.
     *     <li>No slot may appear more than one time as a source slot.
     *     <li>No slot may appear more than one time as a destination slot.
     *     <li>Every slot that appears as a source slot must appear as a destination slot.
     *     <li>If a source slot has an item, it must allow the player to pick it up.
     *     <li>Every destination slot must allow placement of a test item.
     * </ol>
     *
     * @throws InvalidDataException if the slot mapping does not represent a valid reordering for
     *                              the slots in the menu.
     */
    public static void validateSlotMapping(
            ServerPlayer player,
            AbstractContainerMenu menu,
            int[] slotMapping
    ) throws InvalidDataException {
        // Check the length of the slot mapping array
        int minSlots = 2;
        if (slotMapping.length < minSlots) {
            throw new InvalidDataException(String.format(
                    "Slot mapping contains too few slots! Expected at least %d, got %d!",
                    minSlots,
                    slotMapping.length
            ));
        }
        if (slotMapping.length % 2 != 0) {
            throw new InvalidDataException(String.format(
                    "Slot mapping contains an uneven number of slots (%d)!",
                    slotMapping.length
            ));
        }

        // Check that the first slot ID is valid
        validateSlotId(menu, slotMapping[0]);

        // Get the first slot's container
        Container container = menu.slots.get(slotMapping[0]).container;
        IntSet checkedSlots = new IntAVLTreeSet();

        ItemStack testItem = Items.LIGHT.getDefaultInstance();

        // For each pair of slot IDs
        for (int i = 0; i < slotMapping.length; i += 2) {
            int srcId = slotMapping[i];
            int dstId = slotMapping[i + 1];

            // Check that the source slot is valid and in the same container
            validateContainerSlot(menu, srcId, container);

            // Check that the source slot has not been seen before
            if (!checkedSlots.add(srcId)) {
                throw new InvalidDataException(String.format(
                        "Slot mapping contains duplicate source slot %d!",
                        srcId
                ));
            }

            // Check that the destination slot is valid and in the same container
            validateContainerSlot(menu, dstId, container);

            // Transferring items between the same slot is a no-op
            if (srcId == dstId)
                continue;

            // Check that the source slot is accessible
            Slot srcSlot = menu.slots.get(srcId);
            if (srcSlot.hasItem() && !srcSlot.mayPickup(player)) {
                throw new InvalidDataException(String.format(
                        "Slot mapping contains inaccessible source slot %d with item '%s'!",
                        srcId,
                        srcSlot.getItem()
                ));
            }

            // Check that the destination slot is accessible
            Slot dstSlot = menu.slots.get(dstId);
            if (!dstSlot.mayPlace(testItem)) {
                throw new InvalidDataException(String.format(
                        "Slot mapping contains inaccessible destination slot %d with item '%s'!",
                        srcId,
                        dstSlot.getItem()
                ));
            }
        }

        // Check that all destination IDs appear as source IDs exactly once
        for (int i = 1; i < slotMapping.length; i += 2) {
            int dstId = slotMapping[i];
            if (!checkedSlots.remove(dstId)) {
                throw new InvalidDataException(String.format(
                        "Slot mapping contains duplicate destination slot or destination slot that does not appear as source slot (%d)!",
                        dstId
                ));
            }
        }
        if (!checkedSlots.isEmpty()) {
            throw new InvalidDataException(String.format(
                    "Slot mapping contains %d source slots that do not appear as destination slots.",
                    checkedSlots.size()
            ));
        }
    }
}
