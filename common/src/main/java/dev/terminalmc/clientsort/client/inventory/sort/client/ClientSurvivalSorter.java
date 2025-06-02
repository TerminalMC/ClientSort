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

package dev.terminalmc.clientsort.client.inventory.sort.client;

import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;

import static dev.terminalmc.clientsort.client.config.Config.options;

public class ClientSurvivalSorter extends ClientSorter {
    public ClientSurvivalSorter(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        super(screen, screenHelper, originSlot);
    }

    /**
     * Uses mouse click packets to collect partial stacks into the smallest
     * possible number of slots.
     */
    @Override
    protected void collect() {
        // Queue click events before sending to allow cancellation
        ArrayDeque<InteractionManager.InteractionEvent> clickEvents = new ArrayDeque<>();

        // Work backwards from the end, looking for a partial stack
        for (int i = scopeSlots.length - 1; i >= 0; i--) {
            Slot srcSlot = scopeSlots[i];
            ItemStack srcStack = scopeStacks[i];
            if (srcStack.isEmpty()) continue;
            if (srcStack.getCount() >= srcSlot.getMaxStackSize(srcStack)) continue;

            // Partial stack found; pick it up
            clickEvents.add(screenHelper.createClickEvent(
                    scopeSlots[i], 0, ClickType.PICKUP, false));

            // Work forwards from the start, looking for another partial stack
            // of the same item
            for (int j = 0; j < i; j++) {
                Slot dstSlot = scopeSlots[j];
                ItemStack dstStack = scopeStacks[j];
                if (dstStack.isEmpty()) continue;
                if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack)) continue;
                if (!ItemStack.isSameItemSameComponents(srcStack, dstStack)) continue;

                // Matching partial stack found; place as much as possible
                int delta = dstSlot.getMaxStackSize(dstStack) - dstStack.getCount();
                delta = Math.min(delta, srcStack.getCount());

                // Update logical record
                srcStack.setCount(srcStack.getCount() - delta);
                dstStack.setCount(dstStack.getCount() + delta);

                // Send interaction event
                clickEvents.add(screenHelper.createClickEvent(
                        scopeSlots[j], 0, ClickType.PICKUP, false));

                // If no items remain in the source stack, stop looking
                if (srcStack.getCount() <= 0) break;
                // Otherwise keep looking for another matching stack
            }

            // Only pick up the stack if a matching partial stack was found
            if (clickEvents.size() > 1) {
                // Send all click events
                InteractionManager.pushAll(clickEvents);
                InteractionManager.triggerSend(InteractionManager.TriggerType.GUI_CONFIRM);
                clickEvents.clear();

                // Check whether any items are still being carried
                if (srcStack.getCount() > 0) {
                    // Place the carried items back down in their original slot
                    InteractionManager.push(screenHelper.createClickEvent(
                            srcSlot, 0, ClickType.PICKUP, false));
                } else {
                    // Mark the slot as empty
                    scopeStacks[i] = ItemStack.EMPTY;
                }
            }

            // Reset the queue
            clickEvents.clear();
        }
    }

    /**
     * Uses mouse click packets to sort the inventory according to the key.
     */
    @Override
    protected void sort(int[] sortedIds, boolean playSound) {
        ItemStack currentStack;
        final int slotCount = scopeStacks.length;

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
            if (scopeStacks[i].isEmpty()) doneOrEmpty.set(slotCount + i);
        }

        // Bundles require special handling. Specifically, to perform a swap 
        // between the carried item and the target slot, you normally must use
        // left-click (0), but if holding a bundle you must use right-click (1).
        // The current workaround is to maintain a copy of the theoretical
        // inventory state to inform the click decision. This will break if
        // items enter or leave the inventory unexpectedly.
        Item carriedItem = Items.AIR;
        Item[] backingStacks = Arrays.stream(scopeStacks.clone()).map(ItemStack::getItem)
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
                    scopeSlots[sortedIds[i]], 0, ClickType.PICKUP, playSound));
            // Mark the origin slot as empty (because we picked the stack up, duh)
            doneOrEmpty.set(slotCount + sortedIds[i]);
            // Save the stack we're currently working with
            currentStack = scopeStacks[sortedIds[i]];
            // Save a slot that we can use when swapping stacks around
            Slot workingSlot = scopeSlots[sortedIds[i]];

            int id = i; // id will reflect the target slot in the following loop
            do { // This loop follows chained stack moves (e.g. 1->2->5->1)
                if (
                        scopeStacks[id].getItem() == currentStack.getItem()
                                && !doneOrEmpty.get(slotCount + id)
                                && ItemStack.isSameItemSameComponents(scopeStacks[id], currentStack)
                ) {
                    // If the current stack and the target stack are completely
                    // equal, then we can skip this step in the chain
                    if (scopeStacks[id].getCount() == currentStack.getCount()) {
                        doneOrEmpty.set(id); // Mark the current target as done
                        id = origin2Target[id];
                        continue;
                    }
                    if (currentStack.getCount() < scopeStacks[id].getCount()) {
                        // Clicking with a low stack on a full stack does
                        // nothing, so instead we click working slot, target
                        // slot, working, target, working
                        Slot targetSlot = scopeSlots[id];
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

                        currentStack = scopeStacks[id];
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
                            scopeSlots[id], 1, ClickType.PICKUP, playSound));
                } else {
                    InteractionManager.push(screenHelper.createClickEvent(
                            scopeSlots[id], 0, ClickType.PICKUP, playSound));
                }

                // Simulate the swap
                temp = backingStacks[id];
                backingStacks[id] = carriedItem;
                carriedItem = temp;
                currentStack = scopeStacks[id];

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
