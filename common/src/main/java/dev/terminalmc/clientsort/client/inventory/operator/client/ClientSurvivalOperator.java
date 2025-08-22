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

package dev.terminalmc.clientsort.client.inventory.operator.client;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.util.SoundManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayDeque;
import java.util.BitSet;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Provides methods for manipulating the player's inventory or open container via slot-click
 * packets.
 * <p>
 * Valid for use in all screens (and all game-modes) EXCEPT
 * {@link net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen}.
 */
public class ClientSurvivalOperator<T extends Operation> extends ClientOperator<Operation> {

    public ClientSurvivalOperator(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            T operation
    ) {
        super(screen, screenHelper, originSlot, operation);
    }

    /**
     * Uses mouse click packets to collect partial stacks into the smallest possible number of
     * slots.
     */
    @Override
    protected void collect() {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation COLLECT: origin scope is empty!");
            return;
        }
        if (debug())
            ClientSort.LOG.info("Starting operation COLLECT");
        // Queue click events before sending, to allow cancellation
        ArrayDeque<InteractionManager.InteractionEvent> clickEvents = new ArrayDeque<>();

        // Work backwards from the end, looking for a partial stack
        for (int i = originScopeSlots.length - 1; i >= 0; i--) {
            Slot srcSlot = originScopeSlots[i];
            ItemStack srcStack = originScopeStacks[i];

            if (srcStack.isEmpty())
                continue;
            if (srcStack.getCount() >= srcSlot.getMaxStackSize(srcStack))
                continue;

            // Partial stack found; pick it up
            clickEvents.add(screenHelper.createClickEvent(srcSlot, 0, ClickType.PICKUP, false));

            // Work forwards from the start, looking for another partial stack
            // of the same item
            for (int j = 0; j < i; j++) {
                Slot dstSlot = originScopeSlots[j];
                ItemStack dstStack = originScopeStacks[j];

                if (dstStack.isEmpty())
                    continue;
                if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack))
                    continue;
                if (!ItemStack.isSameItemSameComponents(srcStack, dstStack))
                    continue;

                // Matching partial stack found; place as much as possible
                int delta = dstSlot.getMaxStackSize(dstStack) - dstStack.getCount();
                delta = Math.min(delta, srcStack.getCount());

                // Update logical record
                srcStack.shrink(delta);
                dstStack.grow(delta);

                // Queue mouse click
                clickEvents.add(screenHelper.createClickEvent(dstSlot, 0, ClickType.PICKUP, false));

                // If no items remain in the source stack, stop looking
                if (srcStack.getCount() <= 0)
                    break;
                // Otherwise keep looking for another matching stack
            }

            // Only pick up the stack if a slot was found to place it
            if (clickEvents.size() > 1) {
                // Send all mouse clicks
                InteractionManager.pushAll(clickEvents);
                InteractionManager.triggerSend(InteractionManager.TriggerType.GUI_CONFIRM);
                clickEvents.clear();

                // Check whether any items are still being carried
                if (srcStack.getCount() > 0) {
                    // Place the carried items back down in their original slot
                    InteractionManager.push(screenHelper.createClickEvent(
                            srcSlot,
                            0,
                            ClickType.PICKUP,
                            false
                    ));
                } else {
                    // Mark the source slot as empty
                    originScopeStacks[i] = ItemStack.EMPTY;
                }
            }

            // Reset the queue
            clickEvents.clear();
        }
        if (debug()) {
            InteractionManager.push(() -> {
                ClientSort.LOG.info("Finished operation COLLECT");
                return InteractionManager.TICK_WAITER;
            });
        }
    }

    /**
     * Uses mouse click packets to sort the inventory according to the key.
     */
    @Override
    protected void sort(int[] sortedIds, boolean playSound) {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation SORT: origin scope is empty!");
            return;
        }
        if (debug())
            ClientSort.LOG.info("Starting operation SORT");
        final int slotCount = originScopeStacks.length;

        // sortedIds shows, for each slot index, which slot's stack should be
        // moved there.
        // Create an inverted array to show, for each slot index, where the
        // slot's stack should be moved to.
        int[] srcToDst = new int[slotCount];
        for (int i = 0; i < slotCount; i++) {
            srcToDst[sortedIds[i]] = i;
        }

        // Set up a pair of bitsets to track the state of the slots in the
        // simulation
        BitSet done = new BitSet(slotCount);
        BitSet empty = new BitSet(slotCount);
        for (int i = 0; i < slotCount; i++) {
            if (i == sortedIds[i]) {
                // Destination equals source, so the slot does not need to be
                // modified; mark it as done
                done.set(i);
            }
            if (originScopeStacks[i].isEmpty()) {
                // Slot is empty; mark it so
                empty.set(i);
            }
        }

        // Iterate all slots, with i as the destination slot index.
        // sortedIds[i] is therefore the source slot
        for (int i = 0; i < slotCount; i++) {
            // If the destination slot is marked as done, skip
            if (done.get(i))
                continue;

            // If the source slot is marked as empty
            if (empty.get(sortedIds[i])) {
                // Mark it as done, and skip
                done.set(sortedIds[i]);
                continue;
            }

            // Pick up the stack at the source slot,
            InteractionManager.push(screenHelper.createClickEvent(
                    originScopeSlots[sortedIds[i]],
                    0,
                    ClickType.PICKUP,
                    playSound
            ));
            // and update the simulation to match
            ItemStack carriedStack = originScopeStacks[sortedIds[i]];
            originScopeStacks[sortedIds[i]] = ItemStack.EMPTY;
            empty.set(sortedIds[i]);

            // Save an empty slot that we can use when swapping stacks around
            Slot workingSlot = originScopeSlots[sortedIds[i]];

            // This loop follows chained stack moves (e.g. 1->2->5->1), starting
            // from the original destination
            int dstId = i;
            do {
                // If the destination stack is nonempty and matches the carried stack
                if (!empty.get(dstId) && ItemStack.isSameItemSameComponents(
                        carriedStack,
                        originScopeStacks[dstId]
                )) {
                    // If the carried stack exactly equals the destination stack
                    if (carriedStack.getCount() == originScopeStacks[dstId].getCount()) {
                        // No point placing it down, instead mark the destination
                        // as done and move on to the destination's destination
                        done.set(dstId);
                        dstId = srcToDst[dstId];
                        continue;
                    }

                    // If the carried stack is smaller than the destination stack
                    if (carriedStack.getCount() < originScopeStacks[dstId].getCount()) {
                        // Clicking with a low stack on a full stack does
                        // nothing, so we must employ shenanigans
                        Slot dstSlot = originScopeSlots[dstId];

                        // Place down carried stack in working slot
                        InteractionManager.push(screenHelper.createClickEvent(
                                workingSlot,
                                0,
                                ClickType.PICKUP,
                                playSound
                        ));
                        // Pick up destination stack from destination slot
                        InteractionManager.push(screenHelper.createClickEvent(
                                dstSlot,
                                0,
                                ClickType.PICKUP,
                                playSound
                        ));
                        // Place down destination stack on top of working slot,
                        // automatically picking up previously carried stack
                        InteractionManager.push(screenHelper.createClickEvent(
                                workingSlot,
                                0,
                                ClickType.PICKUP,
                                playSound
                        ));
                        // Place down carried stack in destination slot
                        InteractionManager.push(screenHelper.createClickEvent(
                                dstSlot,
                                0,
                                ClickType.PICKUP,
                                playSound
                        ));
                        // Pick up destination stack from working slot
                        InteractionManager.push(screenHelper.createClickEvent(
                                workingSlot,
                                0,
                                ClickType.PICKUP,
                                playSound
                        ));

                        // Swap carried and destination stacks in the simulation
                        ItemStack tmp = carriedStack;
                        carriedStack = originScopeStacks[dstId];
                        originScopeStacks[dstId] = tmp;

                        // Mark the destination as done and move on to the
                        // destination's destination
                        done.set(dstId);
                        dstId = srcToDst[dstId];
                        continue;
                    }
                }
                // At this point, source and destination stacks are known to
                // be non-matching

                // Modify standard click if required for bundles
                int mouseButton = 0;
                boolean clickOnBundleWithItem = originScopeStacks[dstId].is(Items.BUNDLE)
                        && !(carriedStack.isEmpty());
                boolean clickOnItemWithBundle = carriedStack.is(Items.BUNDLE)
                        && !(originScopeStacks[dstId].isEmpty());
                if (options().bundlesUseLeftClick
                        && (clickOnBundleWithItem || clickOnItemWithBundle)) {
                    mouseButton = 1;
                }

                // Swap carried and destination stacks,
                InteractionManager.push(screenHelper.createClickEvent(
                        originScopeSlots[dstId],
                        mouseButton,
                        ClickType.PICKUP,
                        playSound
                ));
                // and update the simulation to match
                ItemStack tmp = carriedStack;
                carriedStack = originScopeStacks[dstId];
                originScopeStacks[dstId] = tmp;

                // Mark the destination as done
                done.set(dstId);

                // If the destination that we just swapped with was empty
                if (empty.get(dstId)) {
                    // We're no longer carrying anything, so break the chain
                    break;
                } else {
                    // Move on to the destination's destination
                    dstId = srcToDst[dstId];
                }

                // If we reach a destination that is marked as done already,
                // then we're finished with this chain
            } while (!done.get(dstId));
        }
        if (debug()) {
            InteractionManager.push(() -> {
                ClientSort.LOG.info("Finished operation SORT");
                return InteractionManager.TICK_WAITER;
            });
        }
    }

    /**
     * Uses mouse click packets to complete as many partial stacks as possible in the other scope
     * using items from the origin scope.
     */
    @Override
    protected void fillStacks() {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation STACK_FILL: origin scope is empty!");
            return;
        }
        if (otherScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation STACK_FILL: other scope is empty!");
            return;
        }
        if (debug())
            ClientSort.LOG.info("Starting operation STACK_FILL");
        raiseFlag();

        // Prepare sounds
        boolean playSound = SoundManager.shouldPlayOtherSounds();
        if (playSound)
            SoundManager.resetForCount(SoundManager.estimateStackFillSounds(
                    originScopeStacks,
                    otherScopeStacks
            ));

        // Queue click events before sending to allow cancellation
        ArrayDeque<InteractionManager.InteractionEvent> clickEvents = new ArrayDeque<>();

        // Work backwards from the end of the source slot array, looking for a
        // nonempty stack
        for (int i = originScopeSlots.length - 1; i >= 0; i--) {
            Slot srcSlot = originScopeSlots[i];
            ItemStack srcStack = originScopeStacks[i];

            if (srcStack.isEmpty())
                continue;

            // Partial stack found; pick it up
            clickEvents.add(screenHelper.createClickEvent(srcSlot, 0, ClickType.PICKUP, false));

            // Work forwards from the start of the destination slot array,
            // looking for a matching partial stack
            for (int j = 0; j < otherScopeSlots.length; j++) {
                Slot dstSlot = otherScopeSlots[j];
                ItemStack dstStack = otherScopeStacks[j];

                if (dstStack.isEmpty())
                    continue;
                if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack))
                    continue;
                if (!ItemStack.isSameItemSameComponents(srcStack, dstStack))
                    continue;

                // Matching partial stack found; place as much as possible
                int delta = dstSlot.getMaxStackSize(dstStack) - dstStack.getCount();
                delta = Math.min(delta, srcStack.getCount());

                // Update logical record
                srcStack.setCount(srcStack.getCount() - delta);
                dstStack.setCount(dstStack.getCount() + delta);

                // Send interaction event
                clickEvents.add(screenHelper.createClickEvent(
                        dstSlot,
                        0,
                        ClickType.PICKUP,
                        playSound
                ));

                // If no items remain in the source stack, stop looking
                if (srcStack.getCount() <= 0)
                    break;
                // Otherwise keep looking for another matching stack
            }

            // Only pick up the stack if we found a place to put it
            if (clickEvents.size() > 1) {
                // Send all click events
                InteractionManager.pushAll(clickEvents);
                InteractionManager.triggerSend(InteractionManager.TriggerType.GUI_CONFIRM);
                clickEvents.clear();

                // Check whether any items are still being carried
                if (srcStack.getCount() > 0) {
                    // Place the carried items back down in their original slot
                    InteractionManager.push(screenHelper.createClickEvent(
                            srcSlot,
                            0,
                            ClickType.PICKUP,
                            false
                    ));
                } else {
                    // Mark the slot as empty
                    originScopeStacks[i] = ItemStack.EMPTY;
                }
            }

            // Reset the queue
            clickEvents.clear();
        }
        lowerFlag();
        if (debug()) {
            InteractionManager.push(() -> {
                ClientSort.LOG.info("Finished operation STACK_FILL");
                return InteractionManager.TICK_WAITER;
            });
        }
    }

    /**
     * Uses mouse click packets to transfer as many items as possible from the origin scope to the
     * other scope, without adding new item types to the destination.
     */
    @Override
    protected void matchTransfer() {
        transfer(collectMatchingSlots(
                originScopeSlots,
                otherScopeStacks,
                options().alwaysMatchByType,
                options().typeMatchItems
        ));
    }

    /**
     * Uses mouse click packets to transfer as many items as possible from the origin scope to the
     * other scope.
     */
    @Override
    protected void transfer() {
        transfer(originScopeSlots);
    }

    protected void transfer(Slot[] originSlots) {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn(
                        "Cannot perform operation (MATCH_)TRANSFER: origin scope is empty!");
            return;
        }
        if (originSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn(
                        "Cannot perform operation (MATCH_)TRANSFER: origin slots is empty!");
            return;
        }
        if (otherScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn(
                        "Cannot perform operation (MATCH_)TRANSFER: other scope is empty!");
            return;
        }
        if (debug())
            ClientSort.LOG.info("Starting operation (MATCH_)TRANSFER");
        raiseFlag();

        // Prepare simulation stack array
        ItemStack[] originStacks = originScopeStacks;
        if (originSlots != originScopeSlots) {
            originStacks = new ItemStack[originSlots.length];
            for (int i = 0; i < originSlots.length; i++) {
                originStacks[i] = originSlots[i].getItem().copy();
            }
        }

        // Prepare sounds
        boolean playSound = SoundManager.shouldPlayOtherSounds();
        if (playSound)
            SoundManager.resetForCount(SoundManager.estimateTransferSounds(
                    originStacks,
                    otherScopeStacks
            ));

        // Queue click events before sending to allow cancellation
        ArrayDeque<InteractionManager.InteractionEvent> clickEvents = new ArrayDeque<>();

        // Work backwards from the end of the source slot array, looking for a
        // nonempty stack
        for (int i = originSlots.length - 1; i >= 0; i--) {
            Slot srcSlot = originSlots[i];
            ItemStack srcStack = originStacks[i];

            if (srcStack.isEmpty())
                continue;

            // Partial stack found; pick it up
            clickEvents.add(screenHelper.createClickEvent(srcSlot, 0, ClickType.PICKUP, false));

            int emptySlotId = -1;

            // Work forwards from the start of the destination slot array,
            // looking for a matching partial stack
            for (int j = 0; j < otherScopeSlots.length; j++) {
                Slot dstSlot = otherScopeSlots[j];
                ItemStack dstStack = otherScopeStacks[j];

                if (dstStack.isEmpty()) {
                    // Save the empty slot
                    if (emptySlotId == -1)
                        emptySlotId = j;
                    continue;
                }
                if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack))
                    continue;
                if (!ItemStack.isSameItemSameComponents(srcStack, dstStack))
                    continue;

                // Matching partial stack found; place as much as possible
                int delta = dstSlot.getMaxStackSize(dstStack) - dstStack.getCount();
                delta = Math.min(delta, srcStack.getCount());

                // Update logical record
                srcStack.setCount(srcStack.getCount() - delta);
                dstStack.setCount(dstStack.getCount() + delta);

                // Send interaction event
                clickEvents.add(screenHelper.createClickEvent(
                        dstSlot,
                        0,
                        ClickType.PICKUP,
                        playSound
                ));

                // If no items remain in the source stack, stop looking
                if (srcStack.isEmpty())
                    break;
                // Otherwise keep looking for another matching stack
            }

            if (!srcStack.isEmpty() && emptySlotId != -1) {
                // Source stack is still not empty; place the remainder down in
                // the saved empty slot
                Slot dstSlot = otherScopeSlots[emptySlotId];

                // Update logical record
                otherScopeStacks[emptySlotId] = srcStack.copy();
                srcStack.setCount(0);

                // Send interaction event
                clickEvents.add(screenHelper.createClickEvent(
                        dstSlot,
                        0,
                        ClickType.PICKUP,
                        playSound
                ));
            }

            // Only pick up the stack if we found a place to put it
            if (clickEvents.size() > 1) {
                // Send all click events
                InteractionManager.pushAll(clickEvents);
                InteractionManager.triggerSend(InteractionManager.TriggerType.GUI_CONFIRM);
                clickEvents.clear();

                // Check whether any items are still being carried
                if (srcStack.getCount() > 0) {
                    // Place the carried items back down in their original slot
                    InteractionManager.push(screenHelper.createClickEvent(
                            srcSlot,
                            0,
                            ClickType.PICKUP,
                            false
                    ));
                } else {
                    // Mark the slot as empty
                    originStacks[i] = ItemStack.EMPTY;
                }
            }

            // Reset the queue
            clickEvents.clear();
        }
        lowerFlag();
        if (debug()) {
            InteractionManager.push(() -> {
                ClientSort.LOG.info("Finished operation (MATCH_)TRANSFER");
                return InteractionManager.TICK_WAITER;
            });
        }
    }
}
