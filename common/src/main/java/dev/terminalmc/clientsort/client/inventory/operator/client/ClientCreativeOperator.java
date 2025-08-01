/*
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
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static dev.terminalmc.clientsort.ClientSort.debug;

/**
 * Provides methods for manipulating the player's inventory or open container via creative mode
 * set-slot packets.
 * <p>
 * Valid for use ONLY in
 * {@link net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen}.
 */
public class ClientCreativeOperator<T extends Operation> extends ClientOperator<Operation> {

    public ClientCreativeOperator(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            T operation
    ) {
        super(screen, screenHelper, originSlot, operation);
    }

    /**
     * Uses creative inventory update packets to collect partial stacks into the smallest possible
     * number of slots.
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

        // Work backwards from the end, looking for a partial stack
        for (int i = originScopeSlots.length - 1; i >= 0; i--) {
            Slot srcSlot = originScopeSlots[i];
            ItemStack srcStack = originScopeStacks[i];

            if (srcStack.isEmpty())
                continue;
            if (srcStack.getCount() >= srcSlot.getMaxStackSize(srcStack))
                continue;

            // Partial stack found; work forwards from the start, looking for
            // another partial stack of the same item
            for (int j = 0; j < i; j++) {
                Slot dstSlot = originScopeSlots[j];
                ItemStack dstStack = originScopeStacks[j];

                if (dstStack.isEmpty())
                    continue;
                if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack))
                    continue;
                if (!ItemStack.isSameItemSameTags(srcStack, dstStack))
                    continue;

                // Matching partial stack found; place as much as possible
                int delta = dstSlot.getMaxStackSize(dstStack) - dstStack.getCount();
                delta = Math.min(delta, srcStack.getCount());

                // Update logical record
                srcStack.shrink(delta);
                dstStack.grow(delta);

                // Send inventory update
                int srcSlotId = ((ISlot) srcSlot).clientsort$getIdInContainer();
                int dstSlotId = ((ISlot) dstSlot).clientsort$getIdInContainer();
                InteractionManager.push(() -> {
                    //noinspection DataFlowIssue
                    Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(
                            srcStack.copy(),
                            srcSlotId
                    );
                    Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(
                            dstStack.copy(),
                            dstSlotId
                    );
                    return InteractionManager.TICK_WAITER;
                });

                // If no items remain in the source stack, stop looking
                if (srcStack.getCount() <= 0)
                    break;
                // Otherwise keep looking for another matching stack
            }
        }
        // Broadcast changes once operation is complete
        InteractionManager.push(() -> {
            //noinspection DataFlowIssue
            Minecraft.getInstance().player.inventoryMenu.broadcastChanges();
            if (debug())
                ClientSort.LOG.info("Finished operation COLLECT");
            return InteractionManager.TICK_WAITER;
        });
    }

    /**
     * Uses creative inventory update packets to sort the inventory according to {@code key}.
     */
    @Override
    protected void sort(int[] key, boolean playSound) {
        if (originScopeSlots.length == 0) {
            if (debug())
                ClientSort.LOG.warn("Cannot perform operation SORT: origin scope is empty!");
            return;
        }
        if (debug())
            ClientSort.LOG.info("Starting operation SORT");
        for (int i = 0; i < key.length; i++) {
            ItemStack srcItem = originScopeStacks[key[i]];
            ItemStack dstItem = originScopeStacks[i];
            if (!srcItem.isEmpty() || !dstItem.isEmpty()) {
                int dstSlotId = ((ISlot) originScopeSlots[i]).clientsort$getIdInContainer();
                InteractionManager.push(() -> {
                    //noinspection DataFlowIssue
                    Minecraft.getInstance().player.inventoryMenu.getSlot(dstSlotId).set(srcItem);
                    //noinspection DataFlowIssue
                    Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(srcItem, dstSlotId);
                    if (playSound)
                        SoundManager.play();
                    return InteractionManager.TICK_WAITER;
                });
            }
        }
        // Broadcast changes once operation is complete
        InteractionManager.push(() -> {
            //noinspection DataFlowIssue
            Minecraft.getInstance().player.inventoryMenu.broadcastChanges();
            if (debug())
                ClientSort.LOG.info("Finished operation SORT");
            return InteractionManager.TICK_WAITER;
        });
    }

    @Override
    protected void transfer() {
        if (debug())
            ClientSort.LOG.warn(
                    "Operation TRANSFER is not supported by {}",
                    this.getClass().getSimpleName()
            );
    }

    @Override
    protected void fillStacks() {
        if (debug())
            ClientSort.LOG.warn(
                    "Operation STACK_FILL is not supported by {}",
                    this.getClass().getSimpleName()
            );
    }
}
