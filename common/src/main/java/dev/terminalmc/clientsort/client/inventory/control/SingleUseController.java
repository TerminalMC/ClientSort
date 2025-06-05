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

package dev.terminalmc.clientsort.client.inventory.control;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.control.client.ClientCreativeController;
import dev.terminalmc.clientsort.client.inventory.control.client.ClientSurvivalController;
import dev.terminalmc.clientsort.client.inventory.util.Scope;
import dev.terminalmc.clientsort.client.inventory.control.server.ServerController;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import java.util.*;

import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Provides methods for manipulating the player's inventory or open container.
 * <p>
 * Note: A {@link SingleUseController} instance must be used one time,
 * immediately after creation, then promptly discarded, because the inventory
 * state is stored on initialization and not updated.
 * </ol>
 */
public abstract class SingleUseController {
    protected boolean hasOperated = false;
    protected final AbstractContainerScreen<?> screen;
    protected final ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper;
    /**
     * The slot that was hovered when sorting was triggered.
     */
    protected final Slot originSlot;
    /**
     * A potentially noncontiguous sub-array of slots in the same scope as
     * {@link SingleUseController#originSlot}.
     * <p>
     * Must NOT be used by client-side operations to track and update simulation
     * state. Instead, use {@link SingleUseController#originScopeStacks}.
     */
    protected final Slot[] originScopeSlots;
    /**
     * A 1:1 equivalent of {@link SingleUseController#originScopeSlots}, keeping
     * a logical record of the stack stored in each slot.
     */
    protected final ItemStack[] originScopeStacks;
    /**
     * A potentially noncontiguous sub-array of slots not in the same scope as
     * {@link SingleUseController#originSlot}, but in still in either
     * {@link Scope#CONTAINER_INV} or {@link Scope#PLAYER_INV}.
     * <p>
     * Must NOT be used by client-side operations to track and update simulation
     * state. Instead, use {@link SingleUseController#originScopeStacks}.
     */
    protected final Slot[] otherScopeSlots;
    /**
     * A 1:1 equivalent of {@link SingleUseController#otherScopeSlots}, keeping
     * a logical record of the stack stored in each slot.
     */
    protected final ItemStack[] otherScopeStacks;

    public SingleUseController(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot
    ) {
        this.screen = screen;
        this.screenHelper = screenHelper;
        this.originSlot = originSlot;

        // Collect slots in origin scope
        Scope originScope = screenHelper.getScope(originSlot);
        originScopeSlots = findSlotsInScope(originScope);
        // Record stacks
        originScopeStacks = new ItemStack[originScopeSlots.length];
        for (int i = 0; i < originScopeSlots.length; i++) {
            originScopeStacks[i] = originScopeSlots[i].getItem();
        }

        // Collect slots in other container scope, if any
        Scope otherScope = switch(originScope) {
            case PLAYER_INV -> Scope.CONTAINER_INV;
            case CONTAINER_INV -> Scope.PLAYER_INV;
            default -> Scope.INVALID;
        };
        otherScopeSlots = findSlotsInScope(otherScope);
        // Record stacks
        otherScopeStacks = new ItemStack[otherScopeSlots.length];
        for (int i = 0; i < otherScopeSlots.length; i++) {
            otherScopeStacks[i] = otherScopeSlots[i].getItem();
        }
    }

    /**
     * Finds all the inventory menu slots that are in {@code scope}.
     */
    private Slot[] findSlotsInScope(Scope scope) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (scope == Scope.INVALID) return new Slot[0];

        ArrayList<Slot> collectedSlots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            // Ignore slots in different scope
            if (screenHelper.getScope(slot) != scope) continue;
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
     * @return {@code true} if this instance has not previously performed an
     * operation (and therefore is able to perform one).
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean canOperate() {
        if (hasOperated) {
            ClientSort.LOG.warn("{} can only be used once!", this.getClass().getSimpleName());
            return false;
        } else {
            hasOperated = true;
            return true;
        }
    }

    /**
     * @return an instance of {@link SingleUseController} optimized for the
     * current game state.
     */
    public static SingleUseController getController(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            CustomPacketPayload.Type<?> payloadType
    ) {
        if (options().serverAcceleratedSorting
                && Services.PLATFORM.canSendToServer(payloadType)
        ) {
            return new ServerController(screen, screenHelper, originSlot);
        }

        //noinspection DataFlowIssue
        if (Minecraft.getInstance().player.isCreative()
                && screen instanceof CreativeModeInventoryScreen
        ) {
            return new ClientCreativeController(screen, screenHelper, originSlot);
        }

        return new ClientSurvivalController(screen, screenHelper, originSlot);
    }

    /**
     * Sorts the inventory according to {@code sortOrder}.
     */
    public abstract void sort(SortOrder sortOrder);

    /**
     * Transfers as many items as possible from the scope of the origin slot
     * to the other container or inventory, if it exists.
     */
    public abstract void transfer();

    /**
     * Uses items in the scope of the origin slot to complete as many partial
     * stacks as possible in the other container or inventory, if it exists.
     */
    public abstract void fillStacks();
}
