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

package dev.terminalmc.clientsort.client.inventory.operator;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.compat.itemlocks.ItemLocksWrapper;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.inventory.operator.client.ClientCreativeOperator;
import dev.terminalmc.clientsort.client.inventory.operator.client.ClientSurvivalOperator;
import dev.terminalmc.clientsort.client.inventory.operator.server.ServerOperator;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.util.Scope;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.ClientServices;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import dev.terminalmc.clientsort.util.SlotLogUtil;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Provides methods for manipulating the player's inventory or open container.
 * <p>
 * Note: A {@link SingleUseOperator} instance must be used one time immediately after creation then
 * promptly discarded, because the inventory state is stored on initialization and never updated.
 * <p>
 * Additionally, due to policy constraints, a {@link SingleUseOperator} is only valid for the type
 * of operation specified when it was created.
 */
public abstract class SingleUseOperator<T extends Operation> {

    private boolean hasOperated = false;
    protected final AbstractContainerScreen<?> screen;
    protected final ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper;
    protected final T operation;
    /**
     * The slot that was hovered when sorting was triggered.
     */
    protected final Slot originSlot;
    /**
     * A potentially noncontiguous sub-array of slots in the same scope as
     * {@link SingleUseOperator#originSlot}.
     * <p>
     * Must NOT be used by client-side operations to track and update simulation state. Instead, use
     * {@link SingleUseOperator#originScopeStacks}.
     */
    protected final Slot[] originScopeSlots;
    /**
     * A 1:1 equivalent of {@link SingleUseOperator#originScopeSlots}, keeping a logical record of
     * the stack stored in each slot.
     */
    protected final ItemStack[] originScopeStacks;
    /**
     * A potentially noncontiguous sub-array of slots not in the same scope as
     * {@link SingleUseOperator#originSlot}, but in still in either {@link Scope#CONTAINER_INV} or
     * {@link Scope#PLAYER_INV}.
     * <p>
     * Must NOT be used by client-side operations to track and update simulation state. Instead, use
     * {@link SingleUseOperator#originScopeStacks}.
     */
    protected final Slot[] otherScopeSlots;
    /**
     * A 1:1 equivalent of {@link SingleUseOperator#otherScopeSlots}, keeping a logical record of
     * the stack stored in each slot.
     */
    protected final ItemStack[] otherScopeStacks;

    public SingleUseOperator(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            T operation
    ) {
        this.screen = screen;
        this.screenHelper = screenHelper;
        this.originSlot = originSlot;
        this.operation = operation;

        // Collect slots in origin scope
        Scope originScope = screenHelper.getScope(originSlot);
        originScopeSlots = collectSlots(originScope);
        if (debug()) {
            ClientSort.LOG.info(
                    "Discovered {} slots in origin scope ({} - {}):",
                    originScopeSlots.length,
                    originScope.ordinal(),
                    originScope.name()
            );
            ClientSort.LOG.info(SlotLogUtil.listSlotIds(List.of(originScopeSlots)));
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
            ClientSort.LOG.info(
                    "Discovered {} slots in other scope ({} - {}):",
                    otherScopeSlots.length,
                    otherScope.ordinal(),
                    otherScope.name()
            );
            ClientSort.LOG.info(SlotLogUtil.listSlotIds(List.of(otherScopeSlots)));
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
            int slotId = ((ISlot) slot).clientsort$getIndexInMenu();
            int slotIdx = ((ISlot) slot).clientsort$getIndexInContainer();
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
            // Ignore ignored slots
            Object object = getObj(slot, screen.getMenu());
            if (object == null)
                continue;
            @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());
            if (policy != null && policy.ignoredSlots().contains(slotIdx))
                continue;
            // Slot is valid
            collectedSlots.add(slot);
        }

        return collectedSlots.toArray(new Slot[0]);
    }

    /**
     * @return {@code true} if this instance was created for the same type of operation.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canPerform(Operation op) {
        if (!operation.equals(op)) {
            ClientSort.LOG.warn(
                    "Cannot perform op {} using an operator created for op {}!",
                    op,
                    operation
            );
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return {@code true} if this instance has not previously performed an operation (and
     * therefore is able to perform one).
     */
    private boolean hasOperated() {
        if (hasOperated) {
            ClientSort.LOG.warn("{} can only be used once!", this.getClass().getSimpleName());
            return true;
        } else {
            hasOperated = true;
            return false;
        }
    }

    /**
     * @return an instance of {@link SingleUseOperator} optimized for the current game state and
     * valid for a single operation of the specified type, or {@code null} if the operation is
     * disallowed by policy.
     */
    public static @Nullable SingleUseOperator<Operation> getOperator(
            AbstractContainerScreen<?> screen,
            ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper,
            Slot originSlot,
            Operation operation,
            boolean onlyClient
    ) {
        // Check policy
        Object object = getObj(originSlot, screen.getMenu());
        if (object == null)
            return null;
        @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());
        if (policy != null) {
            if (!switch (operation) {
                case SORT -> policy.canSort();
                case STACK_FILL -> policy.canStackFill();
                case MATCH_TRANSFER -> policy.canMatchTransfer();
                case TRANSFER -> policy.canTransfer();
            }) {
                if (debug())
                    ClientSort.LOG.warn(
                            "Operation {} is disallowed by policy for class {}!",
                            operation.name(),
                            policy.getClass()
                    );
                return null;
            }
        }

        // Preference server-accelerated ops
        if (!onlyClient && options().useServerAcceleration
                && ClientServices.PLATFORM.canSendToServer(operation.id)) {
            if (debug())
                ClientSort.LOG.info("Preparing server operator for {}", operation.name());
            return new ServerOperator<>(screen, screenHelper, originSlot, operation);
        }

        // Check that there is not already an op running
        if (ClientSort.operatingClient) {
            if (debug())
                ClientSort.LOG.warn(
                        "Client operation is unavailable: another operation is in progress!"
                );
            return null;
        }

        // Select an appropriate client-side operator
        //noinspection DataFlowIssue
        if (Minecraft.getInstance().player.isCreative()
                && screen instanceof CreativeModeInventoryScreen) {
            if (debug())
                ClientSort.LOG.info("Preparing client-creative operator for {}", operation.name());
            return new ClientCreativeOperator<>(screen, screenHelper, originSlot, operation);
        } else {
            if (debug())
                ClientSort.LOG.info("Preparing client-survival operator for {}", operation.name());
            return new ClientSurvivalOperator<>(screen, screenHelper, originSlot, operation);
        }
    }

    /**
     * Sorts the inventory according to {@code sortOrder}.
     */
    public void trySort(SortOrder sortOrder) {
        if (!canPerform(Operation.SORT))
            return;
        if (hasOperated())
            return;
        sort(sortOrder);
    }

    /**
     * Uses items in the scope of the origin slot to complete as many partial stacks as possible in
     * the other container or inventory, if it exists.
     */
    public void tryFillStacks() {
        if (!canPerform(Operation.STACK_FILL))
            return;
        if (hasOperated())
            return;
        fillStacks();
    }

    /**
     * Transfers as many items as possible from the scope of the origin slot to the other container
     * or inventory, if it exists, without adding new item types to the destination.
     */
    public void tryMatchTransfer() {
        if (!canPerform(Operation.MATCH_TRANSFER))
            return;
        if (hasOperated())
            return;
        matchTransfer();
    }

    /**
     * Transfers as many items as possible from the scope of the origin slot to the other container
     * or inventory, if it exists.
     */
    public void tryTransfer() {
        if (!canPerform(Operation.TRANSFER))
            return;
        if (hasOperated())
            return;
        transfer();
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
     * or inventory, if it exists, without adding new item types to the destination.
     */
    protected abstract void matchTransfer();

    /**
     * Transfers as many items as possible from the scope of the origin slot to the other container
     * or inventory, if it exists.
     */
    protected abstract void transfer();

    // Shared utilities

    protected static Slot[] collectMatchingSlots(
            Slot[] originSlots,
            ItemStack[] otherStacks,
            boolean alwaysMatchByType,
            Set<Item> typeMatchItems
    ) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : originSlots) {
            if (containsMatchingStack(
                    otherStacks,
                    slot.getItem(),
                    alwaysMatchByType,
                    typeMatchItems
            )) {
                slots.add(slot);
            }
        }
        return slots.toArray(new Slot[]{});
    }

    protected static boolean containsMatchingStack(
            ItemStack[] stacks,
            ItemStack stack,
            boolean alwaysMatchByType,
            Set<Item> typeMatchItems
    ) {
        for (ItemStack s : stacks) {
            if (stacksMatch(s, stack, alwaysMatchByType, typeMatchItems)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean stacksMatch(
            ItemStack a,
            ItemStack b,
            boolean alwaysMatchByType,
            Set<Item> typeMatchItems
    ) {
        return ItemStack.isSameItemSameTags(a, b) ||
                (
                        ItemStack.isSameItem(a, b)
                                && (alwaysMatchByType || typeMatchItems.contains(a.getItem()))
                );
    }
}
