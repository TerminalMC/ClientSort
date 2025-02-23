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

package dev.terminalmc.clientsort.inventory.sort;

import dev.terminalmc.clientsort.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.clientsort.inventory.ContainerScreenHelper;
import dev.terminalmc.clientsort.main.network.SortPayload;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.platform.Services;
import dev.terminalmc.clientsort.util.SoundManager;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import static dev.terminalmc.clientsort.config.Config.options;

/**
 * Manages inventory sorting actions.
 */
public class InventorySorter {
    private final ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper;
    private final AbstractContainerScreen<?> containerScreen;
    private final Slot[] inventorySlots;
    private final ItemStack[] stacks;

    public InventorySorter(
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            AbstractContainerScreen<?> containerScreen,
            Slot originSlot
    ) {
        this.screenHelper = screenHelper;
        this.containerScreen = containerScreen;

        // Collect valid slots
        this.inventorySlots = collectSlots(originSlot);

        // Create and populate itemStack array from valid slots
        this.stacks = new ItemStack[inventorySlots.length];
        for (int i = 0; i < inventorySlots.length; i++) {
            stacks[i] = inventorySlots[i].getItem();
        }
    }

    /**
     * Collects all slots that are valid for sorting in the context of
     * {@code originSlot}.
     */
    private Slot[] collectSlots(Slot originSlot) {
        Scope originScope = screenHelper.getScope(originSlot);
        if (originScope == Scope.INVALID) return new Slot[0];
        
        ArrayList<Slot> collectedSlots = new ArrayList<>();
        for (Slot slot : containerScreen.getMenu().slots) {
            // Ignore slots in different scope
            if (originScope != screenHelper.getScope(slot)) continue;
            // Ignore locked slots
            if (ItemLocksWrapper.isLocked(slot)) continue;
            // Slot is valid
            collectedSlots.add(slot);
        }

        return collectedSlots.toArray(new Slot[0]);
    }

    /**
     * Stacks stackable items into the smallest possible number of slots.
     */
    private void combineStacks() {
        ItemStack stack;
        ArrayDeque<InteractionManager.InteractionEvent> clickEvents = new ArrayDeque<>();
        // Work backwards from the end, looking for a partial stack
        for (int i = stacks.length - 1; i >= 0; i--) {
            stack = stacks[i];
            if (stack.isEmpty()) continue;
            int stackSize = stack.getCount();
            if (stackSize >= stack.getItem().getDefaultMaxStackSize()) continue;
            // Partial stack found, pick it up
            clickEvents.add(screenHelper.createClickEvent(
                    inventorySlots[i], 0, ClickType.PICKUP, false));
            // Work forwards from the start, looking for another partial stack
            // of the same item
            for (int j = 0; j < i; j++) {
                ItemStack target = stacks[j];
                if (target.isEmpty()) continue;
                if (target.getCount() >= target.getItem().getDefaultMaxStackSize()) continue;
                if (ItemStack.isSameItemSameComponents(stack, target)) {
                    // Matching partial stack found, click on it to place as
                    // much of the carried stack as possible
                    clickEvents.add(screenHelper.createClickEvent(
                            inventorySlots[j], 0, ClickType.PICKUP, false));
                    // Check how many items would be placed by the click, and
                    // update logical record
                    int delta = target.getItem().getDefaultMaxStackSize() - target.getCount();
                    delta = Math.min(delta, stackSize);
                    stackSize -= delta;
                    target.setCount(target.getCount() + delta);
                    // If no items remain in the carried stack, stop looking
                    if (stackSize <= 0) break;
                    // Otherwise keep looking for another matching stack
                }
            }
            // If no matching partial stacks were found, don't bother picking
            // up the stack in the first place
            if (clickEvents.size() <= 1) {
                clickEvents.clear();
                continue;
            }
            // Send all click events
            InteractionManager.pushAll(clickEvents);
            InteractionManager.triggerSend(InteractionManager.TriggerType.GUI_CONFIRM);
            clickEvents.clear();
            // Check whether any items are still being carried
            if (stackSize > 0) {
                // Place the carried items back down in their original slot
                InteractionManager.push(screenHelper.createClickEvent(
                        inventorySlots[i], 0, ClickType.PICKUP, false));
                stack.setCount(stackSize);
            } else {
                // Mark the slot as empty
                stacks[i] = ItemStack.EMPTY;
            }
        }
    }

    /**
     * Sorts the inventory in the specified order according to mod settings.
     */
    public void sort(SortOrder sortOrder) {
        // Check that we actually have something to do
        if (inventorySlots.length <= 1) {
            return;
        }

        // Combine all partial stacks
        combineStacks();
        
        // Create an array of ascending slot numbers
        int[] sortIds = new int[stacks.length];
        for (int i = 0; i < sortIds.length; i++) {
            sortIds[i] = i;
        }
        // Sort the array of slot numbers to make a sorting 'key'
        SortContext context = new SortContext(containerScreen, Arrays.asList(inventorySlots));
        sortIds = sortOrder.sort(sortIds, stacks, context);
        
        if (options().serverAcceleratedSorting && Services.PLATFORM.canSendToServer(SortPayload.TYPE)) {
            // Send the key off to the server
            sortOnServer(sortIds);
        } else {
            // Use click events to sort the inventory according to the key
            boolean playSound = options().soundEnabled && options().soundVolume > 0;
            if (playSound) SoundManager.resetForCount(estimateMaxSounds());
            this.sortOnClient(sortIds, playSound);
        }
    }

    /**
     * Delegates sorting to the server.
     */
    protected void sortOnServer(int[] sortedIds) {
        // Translate the key into a series of swap instructions
        int[] slotMapping = new int[sortedIds.length * 2];
        for (int i = 0; i < sortedIds.length; i++) {
            Slot from = inventorySlots[sortedIds[i]];
            Slot to = inventorySlots[i];
            slotMapping[i * 2] = ((ISlot) from).clientSort$getIdInContainer();
            slotMapping[i * 2 + 1] = ((ISlot) to).clientSort$getIdInContainer();
        }
        screenHelper.translateSlotMapping(slotMapping);
        
        // Send the instructions to the server
        InteractionManager.push(() -> {
            Services.PLATFORM.sendToServer(
                    new SortPayload(containerScreen.getMenu().containerId, slotMapping));
            return InteractionManager.TICK_WAITER;
        });
    }

    /**
     * Estimates the maximum number of sounds for the sort.
     * 
     * <p>Ideally pitch should reach maximum as sorting finishes, so we do a
     * quick calculation to roughly guess the number of sounds (and thus, the
     * number of pitch increments) needed.</p>
     */
    private int estimateMaxSounds() {
        // Count non-empty stacks; assume all these require sorting
        int stackCount = 0;
        for (ItemStack stack : stacks) {
            if (stack != ItemStack.EMPTY) {
                stackCount++;
            }
        }
        // Count 'holes' that will require filling
        int compaction = 0;
        for (int i = 0; i < stackCount; i++) {
            if (stacks[i] == ItemStack.EMPTY) {
                compaction++;
            }
        }
        int size = stackCount + compaction;
        
        // Compensate for a small percentage of swaps requiring multiple clicks
        size += size / 15;
        
        return size;
    }

    /**
     * Uses mouse click events to sort the inventory according to the key array.
     */
    protected void sortOnClient(int[] sortedIds, boolean playSound) {
        ItemStack currentStack;
        final int slotCount = stacks.length;

        // sortedIds maps the slot index (the target id) to which slot's
        // contents should be moved there (the origin id). 
        // Copy this data into a full-sized array.
        int[] origin2Target = new int[slotCount];
        for (int i = 0; i < origin2Target.length; i++) {
            origin2Target[sortedIds[i]] = i;
        }

        // This is a combined bitset to save whether each slot is done or empty.
        // It consists of all bits for the done states in the first half and
        // the empty states in the second half.
        BitSet doneOrEmpty = new BitSet(slotCount * 2);
        // Iterate all slots to set up the state bit set
        for (int i = 0; i < slotCount; i++) {
            // If the target slot is equal to the origin,
            if (i == sortedIds[i]) {
                // then we're done with that slot already.
                doneOrEmpty.set(i);
                continue;
            }
            // Mark if it's empty
            if (stacks[i].isEmpty()) doneOrEmpty.set(slotCount + i);
        }

        // Bundles require special handling. Specifically, to perform a swap 
        // between the carried item and the target slot, you normally must use
        // left-click (0), but if holding a bundle you must use right-click (1).
        // The current workaround is to maintain a copy of the theoretical
        // inventory state to inform the click decision. This will break if
        // items enter or leave the inventory unexpectedly.
        Item carriedItem = Items.AIR;
        Item[] backingStacks = Arrays.stream(stacks.clone()).map(ItemStack::getItem)
                .toArray(Item[]::new);

        // Iterate all slots, with i as the target slot index
        // sortedIds[i] is therefore the origin slot
        for (int i = 0; i < slotCount; i++) {
            // Check if we're already done
            if (doneOrEmpty.get(i)) {
                continue; // Skip
            }
            // Check if the origin is empty
            if (doneOrEmpty.get(slotCount + sortedIds[i])) {
                doneOrEmpty.set(sortedIds[i]); // Mark it as done
                continue; // Skip
            }

            // This is where the action happens.
            Item temp = backingStacks[sortedIds[i]];
            backingStacks[sortedIds[i]] = carriedItem;
            carriedItem = temp;
            // Pick up the stack at the origin slot.
            InteractionManager.push(screenHelper.createClickEvent(
                    inventorySlots[sortedIds[i]], 0, ClickType.PICKUP, playSound));
            // Mark the origin slot as empty (because we picked the stack up, duh)
            doneOrEmpty.set(slotCount + sortedIds[i]);
            // Save the stack we're currently working with
            currentStack = stacks[sortedIds[i]];
            // Save a slot that we can use when swapping stacks around
            Slot workingSlot = inventorySlots[sortedIds[i]];
            
            int id = i; // id will reflect the target slot in the following loop
            do { // This loop follows chained stack moves (e.g. 1->2->5->1)
                if (
                        stacks[id].getItem() == currentStack.getItem()
                        && !doneOrEmpty.get(slotCount + id)
                        && ItemStack.isSameItemSameComponents(stacks[id], currentStack)
                ) {
                    // If the current stack and the target stack are completely
                    // equal, then we can skip this step in the chain
                    if (stacks[id].getCount() == currentStack.getCount()) {
                        doneOrEmpty.set(id); // Mark the current target as done
                        id = origin2Target[id];
                        continue;
                    }
                    if (currentStack.getCount() < stacks[id].getCount()) {
                        // Clicking with a low stack on a full stack does
                        // nothing, so instead we click working slot, target
                        // slot, working, target, working
                        Slot targetSlot = inventorySlots[id];
                        temp = backingStacks[id];
                        backingStacks[id] = carriedItem;
                        carriedItem = temp;
                        InteractionManager.push(screenHelper.createClickEvent(
                                workingSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(
                                targetSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(
                                workingSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(
                                targetSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(
                                workingSlot, 0, ClickType.PICKUP, playSound));

                        currentStack = stacks[id];
                        doneOrEmpty.set(id); // Mark the current target as done
                        id = origin2Target[id];
                        continue;
                    }
                }

                // Swap the current stack with the target stack, using right
                // click if required for bundles
                if (
                        options().lmbBundle && (
                            // Clicking on bundle with item,
                            (backingStacks[id] instanceof BundleItem
                                    && !(carriedItem instanceof AirItem))
                            // or clicking on item with bundle
                            || (carriedItem instanceof BundleItem
                                    && !(backingStacks[id] instanceof AirItem)))
                ) {
                    InteractionManager.push(screenHelper.createClickEvent(
                            inventorySlots[id], 1, ClickType.PICKUP, playSound));
                } else {
                    InteractionManager.push(screenHelper.createClickEvent(
                            inventorySlots[id], 0, ClickType.PICKUP, playSound));
                }
                
                // Simulate the swap
                temp = backingStacks[id];
                backingStacks[id] = carriedItem;
                carriedItem = temp;
                currentStack = stacks[id];
                
                doneOrEmpty.set(id); // Mark the current target as done
                
                // If the target that we just swapped with was empty before,
                // then this breaks the chain.
                if (doneOrEmpty.get(slotCount + id)) {
                    break;
                }
                
                id = origin2Target[id];
                
                // If we find a target that is marked as done already, then we
                // can break the chain.
            } while (!doneOrEmpty.get(id));
        }
    }
}
