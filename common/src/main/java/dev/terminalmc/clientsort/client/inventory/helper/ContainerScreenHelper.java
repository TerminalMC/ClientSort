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

package dev.terminalmc.clientsort.client.inventory.helper;

import dev.terminalmc.clientsort.client.inventory.Scope;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.ticks.ContainerSingleItem;

import java.util.ArrayList;
import java.util.List;

import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Helper for an instance of {@link AbstractContainerScreen}.
 */
public class ContainerScreenHelper<T extends AbstractContainerScreen<?>> {

    protected final T screen;

    protected ContainerScreenHelper(T screen) {
        this.screen = screen;
    }

    /**
     * Creates a {@link ContainerScreenHelper} for {@code screen}.
     */
    public static <T extends AbstractContainerScreen<?>> ContainerScreenHelper<T> of(T screen) {
        if (screen instanceof CreativeModeInventoryScreen creativeScreen) {
            //noinspection unchecked
            return (ContainerScreenHelper<T>) new CreativeContainerScreenHelper<>(creativeScreen);
        }
        return new ContainerScreenHelper<>(screen);
    }

    /**
     * @return {@code true} if the index of the slot in its inventory is less than 9.
     */
    public static boolean isHotbarSlot(Slot slot) {
        return ((ISlot) slot).clientsort$getIndexInContainer() < 9;
    }

    /**
     * @return {@code true} if the index of the slot in its inventory is greater than 35.
     */
    public static boolean isExtraSlot(Slot slot) {
        return ((ISlot) slot).clientsort$getIndexInContainer() > 35;
    }

    /**
     * Gets the scope of the specified {@link Slot}.
     * <p>
     * Scope is a heuristic system for grouping slots together based on their location in the
     * inventory.
     *
     * @param slot the slot for which to get the scope.
     * @return the scope of the slot, or {@link Scope#INVALID} if the slot is not accessible.
     */
    public Scope getScope(Slot slot) {
        //noinspection ConstantValue
        if (slot.container == null)
            return Scope.INVALID;
        if (slot.container instanceof ContainerSingleItem)
            return Scope.INVALID;

        // Screen with only player inventory
        if (screen instanceof EffectRenderingInventoryScreen) {
            // Player inventory
            if (slot.container instanceof Inventory) {
                return getScopeInInventory(slot);
            }
            // Out of inventory e.g. 2x2 crafting grid
            else {
                return Scope.PLAYER_OTHER;
            }
        }
        // Screen with container, and probably player inventory attached
        else {
            // Player inventory
            if (slot.container instanceof Inventory) {
                return getScopeInInventory(slot);
            }
            // Container
            else {
                return Scope.CONTAINER_INV;
            }
        }
    }

    /**
     * Gets the scope of the specified {@link Slot}, assuming the slot's container is already known
     * to be an instance of {@link Inventory}.
     *
     * @param slot the slot for which to get the scope.
     * @return the scope of the slot, which may be {@link Scope#INVALID}.
     */
    private static Scope getScopeInInventory(Slot slot) {
        boolean mergeWithHotbar = false;

        // Extra inventory slots e.g. offhand
        if (isExtraSlot(slot)) {
            switch (options().extraSlotScope) {
                case HOTBAR -> mergeWithHotbar = true;
                case EXTRA -> {
                    return Scope.PLAYER_INV_EXTRA;
                }
                case NONE -> {
                    return Scope.INVALID;
                }
            }
        }

        // Hotbar
        if (mergeWithHotbar || isHotbarSlot(slot)) {
            switch (options().hotbarScope) {
                case HOTBAR -> {
                    return Scope.PLAYER_INV_HOTBAR;
                }
                case NONE -> {
                    return Scope.INVALID;
                }
            }
        }

        return Scope.PLAYER_INV;
    }

    /**
     * Uses {@link ContainerScreenHelper#getSlotGroups} to find the largest group of slots.
     *
     * @param scope the scope from which to collect slots.
     * @return the largest group of slots, which may be empty.
     */
    public List<Slot> getLargestSlotGroup(Scope scope) {
        List<Slot> slots = new ArrayList<>();
        for (List<Slot> group : getSlotGroups(scope)) {
            if (group.size() > slots.size()) {
                slots = group;
            }
        }
        return slots;
    }

    /**
     * Uses {@link ContainerScreenHelper#getAllSlots} or {@link ContainerScreenHelper#getSlotGroups}
     * (depending on context) to find the group of slots containing the specified slot.
     *
     * @param slot  the slot for which to find a group.
     * @param scope the scope of the slot.
     * @return the group containing the slot, or an empty list if no group was found.
     */
    public List<Slot> getGroupForSlot(Slot slot, Scope scope) {
        if (scope == Scope.PLAYER_INV || scope == Scope.PLAYER_INV_HOTBAR) {
            // Player inventory and hotbar use custom scoping options so may be required to include
            // slots not normally grouped together
            List<Slot> slots = getAllSlots(slot.container, scope);
            if (slots.contains(slot)) {
                return slots;
            }
        } else {
            for (List<Slot> group : getSlotGroups(scope)) {
                if (group.contains(slot)) {
                    return group;
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * Scans the screen menu's slots and groups them by container and ascending container index.
     * <p>
     * Each group is guaranteed to only contain slots that share a non-null container and have
     * unique container indexes that ascend (not necessarily uniformly) in menu iteration order.
     * However, multiple groups may contain slots from the same container, if the container index
     * descends or stays the same for two consecutive slots that share a non-null container.
     * <p>
     * Groups are contiguous in iteration order, with two exceptions: slots with a null container
     * and slots that are out of scope are skipped but do not force creation of a new group, so they
     * appear as 'gaps' in a group's sequence.
     *
     * @param scope the scope to collect slots from.
     * @return the list of slot groups.
     */
    public List<List<Slot>> getSlotGroups(Scope scope) {
        List<List<Slot>> slotGroups = new ArrayList<>();

        List<Slot> currentGroup = new ArrayList<>();
        int lastIdx = Integer.MAX_VALUE;
        Container lastContainer = null;

        for (Slot slot : screen.getMenu().slots) {
            // Ignore irrelevant slots
            //noinspection ConstantValue
            if (slot.container == null)
                continue;
            if (!getScope(slot).equals(scope))
                continue;

            int slotIdx = ((ISlot) slot).clientsort$getIndexInContainer();
            if (slotIdx <= lastIdx || slot.container != lastContainer) {
                if (!currentGroup.isEmpty())
                    slotGroups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(slot);
            lastIdx = slotIdx;
            lastContainer = slot.container;
        }
        if (!currentGroup.isEmpty())
            slotGroups.add(currentGroup);

        return slotGroups;
    }

    /**
     * Scans the screen menu's slots and collects all slots with the specified scope and a non-null
     * container equal to the specified container.
     *
     * @param container the context container.
     * @param scope     the scope to collect slots from.
     * @return the list of matching slots.
     */
    public List<Slot> getAllSlots(Container container, Scope scope) {
        List<Slot> slots = new ArrayList<>();

        for (Slot slot : screen.getMenu().slots) {
            // Ignore irrelevant slots
            //noinspection ConstantValue
            if (slot.container == null)
                continue;
            if (!getScope(slot).equals(scope))
                continue;
            // Ignore slots from a different container
            if (slot.container != container)
                continue;

            slots.add(slot);
        }

        return slots;
    }

    /**
     * Workaround for inconsistency between client-side and server-side inventory sizes (and
     * therefore slot indexes).
     */
    public void translateSlotIds(int[] slotMapping) {
    }
}
