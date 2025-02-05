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

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.clientsort.inventory.ContainerScreenHelper;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.SoundUtil;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import static dev.terminalmc.clientsort.config.Config.options;

public class InventorySorter {
    private final ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper;
    private final AbstractContainerScreen<?> containerScreen;
    private Slot[] inventorySlots;
    private final ItemStack[] stacks;

    public InventorySorter(ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper, AbstractContainerScreen<?> containerScreen, Slot originSlot) {
        this.screenHelper = screenHelper;
        this.containerScreen = containerScreen;

        collectSlots(originSlot);

        this.stacks = new ItemStack[inventorySlots.length];
        for (int i = 0; i < inventorySlots.length; i++) {
            stacks[i] = inventorySlots[i].getItem();
        }
    }

    private void collectSlots(Slot originSlot) {
        int originScope = screenHelper.getScope(originSlot);
        if (originScope == ContainerScreenHelper.INVALID_SCOPE) {
            this.inventorySlots = new Slot[0];
            return;
        }
        ArrayList<Slot> slotsInScope = new ArrayList<>();
        for (Slot slot : containerScreen.getMenu().slots) {
            if (originScope == screenHelper.getScope(slot, true)) {
                if (!ItemLocksWrapper.isLocked(slot)) {
                    slotsInScope.add(slot);
                }
            }
        }
        this.inventorySlots = slotsInScope.toArray(new Slot[0]);
    }

    private void combineStacks() {
        ItemStack stack;
        ArrayDeque<InteractionManager.InteractionEvent> clickEvents = new ArrayDeque<>();
        for (int i = stacks.length - 1; i >= 0; i--) {
            stack = stacks[i];
            if (stack.isEmpty()) continue;
            int stackSize = stack.getCount();
            if (stackSize >= stack.getItem().getDefaultMaxStackSize()) continue;
            clickEvents.add(screenHelper.createClickEvent(inventorySlots[i], 0, ClickType.PICKUP, false));
            for (int j = 0; j < i; j++) {
                ItemStack targetStack = stacks[j];
                if (targetStack.isEmpty()) continue;
                if (targetStack.getCount() >= targetStack.getItem().getDefaultMaxStackSize()) continue;
                if (ItemStack.isSameItemSameComponents(stack, targetStack)) {
                    int delta = targetStack.getItem().getDefaultMaxStackSize() - targetStack.getCount();
                    delta = Math.min(delta, stackSize);
                    stackSize -= delta;
                    targetStack.setCount(targetStack.getCount() + delta);
                    clickEvents.add(screenHelper.createClickEvent(inventorySlots[j], 0, ClickType.PICKUP, false));
                    if (stackSize <= 0) break;
                }
            }
            if (clickEvents.size() <= 1) {
                clickEvents.clear();
                continue;
            }
            InteractionManager.pushAll(clickEvents);
            InteractionManager.triggerSend(InteractionManager.TriggerType.GUI_CONFIRM);
            clickEvents.clear();
            if (stackSize > 0) {
                InteractionManager.push(screenHelper.createClickEvent(inventorySlots[i], 0, ClickType.PICKUP, false));
                stack.setCount(stackSize);
            } else {
                stacks[i] = ItemStack.EMPTY;
            }
        }
    }

    public void sort(SortMode sortMode) {
        if (inventorySlots.length <= 1) {
            return;
        }

        combineStacks();
        int[] sortIds = new int[stacks.length];
        for (int i = 0; i < sortIds.length; i++) {
            sortIds[i] = i;
        }

        sortIds = sortMode.sort(sortIds, stacks, new SortContext(containerScreen, Arrays.asList(inventorySlots)));
        
        boolean playSound = options().soundEnabled && options().soundVolume > 0;
        if (playSound) SoundUtil.reset(getSoundCount());
        
        this.sortOnClient(sortIds, playSound);
    }
    
    private int getSoundCount() {
        // We want the pitch to reach maximum as sorting finishes, so we
        // do a quick calculation to estimate the rough number of operations
        int stackCount = 0;
        for (ItemStack stack : stacks) {
            if (stack != ItemStack.EMPTY) {
                stackCount++;
            }
        }
        int compaction = 0;
        for (int i = 0; i < stackCount; i++) {
            if (stacks[i] == ItemStack.EMPTY) {
                compaction++;
            }
        }
        int size = stackCount + compaction;
        
        // Roughly compensate for swaps requiring multiple operations
        size += size / 15;
        
        return size;
    }

    protected void sortOnClient(int[] sortedIds, boolean playSound) {
        ItemStack currentStack;
        final int slotCount = stacks.length;

        // sortedIds now maps the slot index (the target id) to which slot's contents should be moved there (the origin id)
        int[] origin2Target = new int[slotCount];
        for (int i = 0; i < origin2Target.length; i++) {
            origin2Target[sortedIds[i]] = i;
        }

        // This is a combined bitset to save whether eac slot is done or empty.
        // It consists of all bits for the done states in the first half and the empty states in the second half.
        BitSet doneSlashEmpty = new BitSet(slotCount * 2);
        for (int i = 0; i < slotCount; i++) { // Iterate all slots to set up the state bit set
            if (i == sortedIds[i]) { // If the target slot is equal to the origin,
                doneSlashEmpty.set(i); // then we're done with that slot already.
                continue;
            }
            if (stacks[i].isEmpty()) doneSlashEmpty.set(slotCount + i); // mark if it's empty
        }

        // Bundles require special handling. Specifically, to perform a swap between the carried
        // item and the target slot, you normally use left-click (0), but if holding a bundle
        // you must use right-click (1).
        // It isn't possible to always use right-click because right-clicking a bundle on an empty
        // slot does nothing, and right-clicking on a stack while carrying nothing takes half.
        // The current workaround is to maintain a copy of the theoretical inventory state to inform
        // the click decision. This will break if items enter or leave the inventory unexpectedly.
        Item carriedItem = Items.AIR;
        Item[] backingStacks = Arrays.stream(stacks.clone()).map(ItemStack::getItem).toArray(Item[]::new);

        // Iterate all slots, with i as the target slot index
        // sortedIds[i] is therefore the origin slot
        for (int i = 0; i < slotCount; i++) {
            if (doneSlashEmpty.get(i)) { // See if we're already done,
                continue; // and skip.
            }
            if (doneSlashEmpty.get(slotCount + sortedIds[i])) { // If the origin is empty,
                doneSlashEmpty.set(sortedIds[i]); // we can mark it as done
                continue; // and skip.
            }

            // This is where the action happens.
            // Pick up the stack at the origin slot.
            Item temp = backingStacks[sortedIds[i]];
            backingStacks[sortedIds[i]] = carriedItem;
            carriedItem = temp;
            InteractionManager.push(screenHelper.createClickEvent(inventorySlots[sortedIds[i]], 0, ClickType.PICKUP, playSound));
            doneSlashEmpty.set(slotCount + sortedIds[i]); // Mark the origin slot as empty (because we picked the stack up, duh)
            currentStack = stacks[sortedIds[i]]; // Save the stack we're currently working with
            Slot workingSlot = inventorySlots[sortedIds[i]]; // A slot that we can use when fiddling around with swapping stacks
            int id = i; // id will reflect the target slot in the following loop
            do { // This loop follows chained stack moves (e.g. 1->2->5->1).
                if (
                        stacks[id].getItem() == currentStack.getItem()
                                //&& stacks[id].getCount() == currentStack.getCount()
                                && !doneSlashEmpty.get(slotCount + id)
                                && ItemStack.isSameItemSameComponents(stacks[id], currentStack)
                ) {
                    // If the current stack and the target stack are completely equal, then we can skip this step in the chain
                    if (stacks[id].getCount() == currentStack.getCount()) {
                        doneSlashEmpty.set(id); // mark the current target as done
                        id = origin2Target[id];
                        continue;
                    }
                    if (currentStack.getCount() < stacks[id].getCount()) { // Clicking with a low stack on a full stack does nothing
                        // The workaround is: click working slot, click target slot, click working slot, click target slot, click working slot
                        Slot targetSlot = inventorySlots[id];
                        temp = backingStacks[id];
                        backingStacks[id] = carriedItem;
                        carriedItem = temp;
                        InteractionManager.push(screenHelper.createClickEvent(workingSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(targetSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(workingSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(targetSlot, 0, ClickType.PICKUP, playSound));
                        InteractionManager.push(screenHelper.createClickEvent(workingSlot, 0, ClickType.PICKUP, playSound));

                        currentStack = stacks[id];
                        doneSlashEmpty.set(id); // mark the current target as done
                        id = origin2Target[id];
                        continue;
                    }
                }

                // swap the current stack with the target stack
                if (
                        options().lmbBundle
                        && (
                                (backingStacks[id] instanceof BundleItem && !(carriedItem instanceof AirItem))
                                || (carriedItem instanceof BundleItem && !(backingStacks[id] instanceof AirItem))
                        )
                ) {
                    InteractionManager.push(screenHelper.createClickEvent(inventorySlots[id], 1, ClickType.PICKUP, playSound));
                } else {
                    InteractionManager.push(screenHelper.createClickEvent(inventorySlots[id], 0, ClickType.PICKUP, playSound));
                }
                temp = backingStacks[id];
                backingStacks[id] = carriedItem;
                carriedItem = temp;
                currentStack = stacks[id];
                doneSlashEmpty.set(id); // mark the current target as done
                // If the target that we just swapped with was empty before, then this breaks the chain.
                if (doneSlashEmpty.get(slotCount + id)) {
                    break;
                }
                id = origin2Target[id];
            } while (!doneSlashEmpty.get(id)); // If we find a target that is marked as done already, then we can break the chain.
        }
    }
}
