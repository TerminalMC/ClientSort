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

package dev.terminalmc.clientsort.client.inventory.sort;

import dev.terminalmc.clientsort.client.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.util.Scope;
import dev.terminalmc.clientsort.client.inventory.sort.client.ClientCreativeSorter;
import dev.terminalmc.clientsort.client.inventory.sort.client.ClientSurvivalSorter;
import dev.terminalmc.clientsort.client.inventory.sort.server.ServerSorter;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.Services;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import java.util.*;

import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Manages inventory sorting actions.
 */
public abstract class InventorySorter {
    protected final AbstractContainerScreen<?> screen;
    protected final ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper;
    /**
     * A potentially noncontiguous sub-array of inventory slots to sort.
     */
    protected final Slot[] scopeSlots;
    /**
     * A logical record of the items stored in
     * {@link InventorySorter#scopeSlots}.
     * <p> 
     * Any index valid in {@link InventorySorter#scopeSlots} is also valid here.
     * <p>
     * Must be used by iterative client-side operations instead of
     * {@code scopeSlots[i].getItem()}.
     */
    protected final ItemStack[] scopeStacks;
    /**
     * The slot that was hovered when sorting was triggered.
     */
    protected final Slot originSlot;

    public InventorySorter(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        this.screen = screen;
        this.screenHelper = screenHelper;
        this.originSlot = originSlot;

        // Collect slots in scope
        scopeSlots = findSlotsInScope(originSlot);

        // Record stacks for slots
        scopeStacks = new ItemStack[scopeSlots.length];
        for (int i = 0; i < scopeSlots.length; i++) {
            scopeStacks[i] = scopeSlots[i].getItem();
        }
    }

    /**
     * Finds all slots that are valid for sorting in the scope of
     * {@code originSlot}.
     */
    private Slot[] findSlotsInScope(Slot originSlot) {
        LocalPlayer player = Minecraft.getInstance().player;
        Scope originScope = screenHelper.getScope(originSlot);
        if (originScope == Scope.INVALID) return new Slot[0];

        ArrayList<Slot> collectedSlots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            // Ignore slots in different scope
            if (originScope != screenHelper.getScope(slot)) continue;
            // Ignore inaccessible slots
            if (player != null && !slot.mayPickup(player)) continue;
            // Ignore locked slots
            if (ItemLocksWrapper.isLocked(slot)) continue;
            // Slot is valid
            collectedSlots.add(slot);
        }

        return collectedSlots.toArray(new Slot[0]);
    }

    /**
     * Sorts the inventory in the specified order according to mod settings.
     */
    public abstract void sort(SortOrder sortOrder);

    public static InventorySorter getSorter(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        if (
                options().serverAcceleratedSorting
                && Services.PLATFORM.canSendToServer(CollectPayload.TYPE)
                && Services.PLATFORM.canSendToServer(SortPayload.TYPE)
        ) {
            return new ServerSorter(screen, screenHelper, originSlot);
        }

        //noinspection DataFlowIssue
        if (
                Minecraft.getInstance().player.isCreative()
                && screen instanceof CreativeModeInventoryScreen
        ) {
            return new ClientCreativeSorter(screen, screenHelper, originSlot);
        }

        return new ClientSurvivalSorter(screen, screenHelper, originSlot);
    }
}
