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

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.clientsort.client.gui.ControlButtonManager;
import dev.terminalmc.clientsort.client.inventory.control.client.ClientCreativeController;
import dev.terminalmc.clientsort.client.inventory.control.client.ClientSurvivalController;
import dev.terminalmc.clientsort.client.inventory.control.server.ServerController;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.util.Scope;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.ClientServices;
import dev.terminalmc.clientsort.config.ClassPolicy;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.TransferPayload;
import dev.terminalmc.clientsort.util.SlotLogUtil;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Provides methods for manipulating the player's inventory or open container.
 * <p>
 * Note: A {@link SingleUseController} instance must be used one time immediately after creation
 * then promptly discarded, because the inventory state is stored on initialization and never
 * updated.
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
     * Must NOT be used by client-side operations to track and update simulation state. Instead, use
     * {@link SingleUseController#originScopeStacks}.
     */
    protected final Slot[] originScopeSlots;
    /**
     * A 1:1 equivalent of {@link SingleUseController#originScopeSlots}, keeping a logical record of
     * the stack stored in each slot.
     */
    protected final ItemStack[] originScopeStacks;
    /**
     * A potentially noncontiguous sub-array of slots not in the same scope as
     * {@link SingleUseController#originSlot}, but in still in either {@link Scope#CONTAINER_INV} or
     * {@link Scope#PLAYER_INV}.
     * <p>
     * Must NOT be used by client-side operations to track and update simulation state. Instead, use
     * {@link SingleUseController#originScopeStacks}.
     */
    protected final Slot[] otherScopeSlots;
    /**
     * A 1:1 equivalent of {@link SingleUseController#otherScopeSlots}, keeping a logical record of
     * the stack stored in each slot.
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
        originScopeSlots = collectSlots(originScope);
        if (debug()) {
            ClientSort.LOG.warn("Origin Scope Slot IDs ({})", originScopeSlots.length);
            SlotLogUtil.logSlotIds(List.of(originScopeSlots));
        }
        // Record stacks
        originScopeStacks = new ItemStack[originScopeSlots.length];
        for (int i = 0; i < originScopeSlots.length; i++) {
            originScopeStacks[i] = originScopeSlots[i].getItem().copy();
        }

        // Collect slots in other container scope, if any
        Scope otherScope = switch (originScope) {
            case PLAYER_INV -> Scope.CONTAINER_INV;
            case CONTAINER_INV -> Scope.PLAYER_INV;
            default -> Scope.INVALID;
        };
        otherScopeSlots = collectSlots(otherScope);
        if (debug()) {
            ClientSort.LOG.warn("Other Scope Slot IDs ({})", otherScopeSlots.length);
            SlotLogUtil.logSlotIds(List.of(otherScopeSlots));
        }
        // Record stacks
        otherScopeStacks = new ItemStack[otherScopeSlots.length];
        for (int i = 0; i < otherScopeSlots.length; i++) {
            otherScopeStacks[i] = otherScopeSlots[i].getItem().copy();
        }
    }

    /**
     * Finds all the valid inventory menu slots that are in {@code scope}.
     */
    private Slot[] collectSlots(Scope scope) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (scope == Scope.INVALID)
            return new Slot[0];

        ItemStack testItem = Items.LIGHT.getDefaultInstance();
        ArrayList<Slot> collectedSlots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            int slotId = ((ISlot) slot).clientsort$getIdInContainer();
            // Ignore slots in different scope
            if (screenHelper.getScope(slot) != scope)
                continue;
            // Ignore inaccessible slots
            if (slot.hasItem()) {
                // Nonempty slot; check pickup
                if (player != null && !slot.mayPickup(player))
                    continue;
            } else {
                // Empty slot; check arbitrary item placement
                if (!slot.container.canPlaceItem(slotId, testItem) || !slot.mayPlace(testItem))
                    continue;
            }
            // Ignore locked slots
            if (ItemLocksWrapper.isLocked(slot))
                continue;
            // Slot is valid
            collectedSlots.add(slot);
        }

        return collectedSlots.toArray(new Slot[0]);
    }

    /**
     * @return {@code true} if this instance has not previously performed an operation (and
     * therefore is able to perform one).
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
     * @return an instance of {@link SingleUseController} optimized for the current game state.
     */
    public static @Nullable SingleUseController getController(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            Type<?> payloadType
    ) {
        // Check policies
        if (options().applyPolicies) {
            Object object = originSlot.container instanceof SimpleContainer
                    ? screen.getMenu()
                    : originSlot.container;
            ClassPolicy policy = ControlButtonManager.getPolicy(object.getClass());
            if (policy != null && policyDisablesType(policy, payloadType))
                return null;
        }

        // Preference server-accelerated ops
        if (options().useServerAcceleration
                && ClientServices.PLATFORM.canSendToServer(payloadType)) {
            return new ServerController(screen, screenHelper, originSlot);
        }

        // Check that there is not already an op running
        if (ClientSort.operatingClient)
            return null;

        // Select an appropriate client-side operator
        //noinspection DataFlowIssue
        if (Minecraft.getInstance().player.isCreative()
                && screen instanceof CreativeModeInventoryScreen) {
            return new ClientCreativeController(screen, screenHelper, originSlot);
        } else {
            return new ClientSurvivalController(screen, screenHelper, originSlot);
        }
    }

    public static boolean policyDisablesType(ClassPolicy policy, Type<?> payloadType) {
        if (payloadType.equals(SortPayload.TYPE) || payloadType.equals(CollectPayload.TYPE)) {
            return !policy.sortEnabled;
        } else if (payloadType.equals(StackFillPayload.TYPE)) {
            return !policy.stackFillEnabled;
        } else if (payloadType.equals(TransferPayload.TYPE)) {
            return !policy.transferEnabled;
        } else {
            throw new IllegalArgumentException("Invalid payload type '%s'".formatted(payloadType));
        }
    }

    /**
     * If allowed by policy, sorts the inventory according to {@code sortOrder}.
     */
    public void trySort(SortOrder sortOrder) {
        if (!policyAllowsOp(originScopeSlots, (p) -> p.sortEnabled))
            return;
        sort(sortOrder);
    }

    /**
     * If allowed by policy, uses items in the scope of the origin slot to complete as many partial
     * stacks as possible in the other container or inventory, if it exists.
     */
    public void tryFillStacks() {
        if (!policyAllowsOp(originScopeSlots, (p) -> p.stackFillEnabled))
            return;
        if (!policyAllowsOp(otherScopeSlots, (p) -> p.stackFillEnabled))
            return;
        fillStacks();
    }

    /**
     * If allowed by policy, transfers as many items as possible from the scope of the origin slot
     * to the other container or inventory, if it exists.
     */
    public void tryTransfer() {
        if (!policyAllowsOp(originScopeSlots, (p) -> p.transferEnabled))
            return;
        if (!policyAllowsOp(otherScopeSlots, (p) -> p.transferEnabled))
            return;
        transfer();
    }

    /**
     * @return {@code true} if there exists a policy disallowing this operation in this context.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean policyAllowsOp(Slot[] slots, Function<ClassPolicy, Boolean> check) {
        if (slots.length == 0)
            return false;
        if (options().applyPolicies) {
            Object object = slots[0].container instanceof SimpleContainer
                    ? screen.getMenu()
                    : slots[0].container;
            ClassPolicy policy = ControlButtonManager.getPolicy(object.getClass());
            return policy == null || check.apply(policy);
        }
        return false;
    }

    /**
     * Sorts the inventory according to {@code sortOrder}.
     */
    protected abstract void sort(SortOrder sortOrder);

    /**
     * Uses items in the scope of the origin slot to complete as many partial stacks as possible in
     * the other container or inventory, if it exists.
     */
    protected abstract void fillStacks();

    /**
     * Transfers as many items as possible from the scope of the origin slot to the other container
     * or inventory, if it exists.
     */
    protected abstract void transfer();
}
