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

package dev.terminalmc.clientsort.client.inventory.control.client;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.sound.SoundManager;
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
public class ClientCreativeController extends ClientController {

    public ClientCreativeController(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        super(screen, screenHelper, originSlot);
    }

    /**
     * Uses creative inventory update packets to collect partial stacks into the smallest possible
     * number of slots.
     */
    @Override
    protected void collect() {
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
                if (!ItemStack.isSameItemSameComponents(srcStack, dstStack))
                    continue;

                // Matching partial stack found; place as much as possible
                int delta = dstSlot.getMaxStackSize(dstStack) - dstStack.getCount();
                delta = Math.min(delta, srcStack.getCount());

                // Update logical record
                srcStack.shrink(delta);
                dstStack.grow(delta);

                // Send inventory update
                int dstSlotId = ((ISlot) dstSlot).clientsort$getIdInContainer();
                InteractionManager.push(() -> {
                    //noinspection DataFlowIssue
                    Minecraft.getInstance().player.inventoryMenu
                            .getSlot(dstSlotId)
                            .set(srcStack.copy());
                    //noinspection DataFlowIssue
                    Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(
                            srcStack.copy(),
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
            return InteractionManager.TICK_WAITER;
        });
    }

    /**
     * Uses creative inventory update packets to sort the inventory according to {@code key}.
     */
    @Override
    protected void sort(int[] key, boolean playSound) {
        for (int i = 0; i < key.length; i++) {
            ItemStack srcItem = originScopeStacks[key[i]];
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
        // Broadcast changes once operation is complete
        InteractionManager.push(() -> {
            //noinspection DataFlowIssue
            Minecraft.getInstance().player.inventoryMenu.broadcastChanges();
            return InteractionManager.TICK_WAITER;
        });
    }

    @Override
    protected void transfer() {
        if (!canOperate())
            return;
        if (debug())
            ClientSort.LOG.warn("Transfer is not supported by {}", this.getClass().getSimpleName());
    }

    @Override
    protected void fillStacks() {
        if (!canOperate())
            return;
        if (debug())
            ClientSort.LOG.warn(
                    "Stack fill is not supported by {}",
                    this.getClass().getSimpleName()
            );
    }
}
