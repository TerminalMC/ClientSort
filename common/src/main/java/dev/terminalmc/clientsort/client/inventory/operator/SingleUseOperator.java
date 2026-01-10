/*
 * Copyright 2022 Siphalor
 * Copyright 2026 TerminalMC
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
import dev.terminalmc.clientsort.client.compat.itemlocks.ItemLocksCompat;
import dev.terminalmc.clientsort.client.compat.stacktnc.StackTncCompat;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Operation;
import dev.terminalmc.clientsort.client.gui.TriggerButtonManager;
import dev.terminalmc.clientsort.client.interaction.InteractionManager;
import dev.terminalmc.clientsort.client.inventory.Scope;
import dev.terminalmc.clientsort.client.inventory.helper.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.operator.client.ClientCreativeOperator;
import dev.terminalmc.clientsort.client.inventory.operator.client.ClientOperator;
import dev.terminalmc.clientsort.client.inventory.operator.client.ClientSurvivalOperator;
import dev.terminalmc.clientsort.client.inventory.operator.server.ServerOperator;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.platform.services.ClientPlatformServices;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

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
public abstract class SingleUseOperator {

    protected final AbstractContainerScreen<?> screen;
    protected final ContainerScreenHelper<? extends AbstractContainerScreen<?>> screenHelper;
    protected final Operation op;
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

    protected SingleUseOperator(AbstractContainerScreen<?> screen, Slot originSlot, Operation op) {
        this.screen = screen;
        this.screenHelper = ContainerScreenHelper.of(screen);
        this.originSlot = originSlot;
        this.op = op;

        // Collect slots in origin scope
        Scope originScope = screenHelper.getScope(originSlot);
        originScopeSlots = collectSlots(originSlot, originScope);
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

        // Get the other container, if any
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        @Nullable Slot otherSlot = originSlot.container == player.getInventory()
                ? TriggerButtonManager.getContainerRefSlot(op)
                : TriggerButtonManager.getPlayerRefSlot(op);
        if (otherSlot == null || !op.isDirectional()) {
            otherScopeSlots = new Slot[]{};
            otherScopeStacks = new ItemStack[]{};
            return;
        }

        // Collect slots in other container scope, if any
        Scope otherScope = screenHelper.getScope(otherSlot);
        otherScopeSlots = collectSlots(otherSlot, otherScope);
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
     *
     * @param refSlot the reference slot.
     * @param scope   the scope of the reference slot.
     */
    private Slot[] collectSlots(Slot refSlot, Scope scope) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (scope == Scope.INVALID)
            return new Slot[0];

        ItemStack testItem = Items.LIGHT.getDefaultInstance();
        ArrayList<Slot> collectedSlots = new ArrayList<>();
        for (Slot slot : screenHelper.getGroupForSlot(refSlot, scope)) {
            int slotId = ((ISlot) slot).clientsort$getIndexInMenu();
            int slotIdx = ((ISlot) slot).clientsort$getIndexInContainer();
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
            // Ignore mod-locked slots
            if (ItemLocksCompat.isLocked(slot))
                continue;
            if (StackTncCompat.isLocked(slot))
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
     * Sets a flag to indicate that a client interaction operation is in progress.
     */
    protected static void startClientOp() {
        // Raise flag
        ClientSort.operatingClient = true;
    }

    /**
     * Queues an interaction event to clear the flag set by {@link ClientOperator#startClientOp()},
     * and run the next queued operation, if there is any.
     */
    protected static void endClientOp() {
        InteractionManager.push(() -> {
            // This op has now sent its last interaction, check for a queued op
            Runnable nextOp = ClientSort.clientOpQueue.poll();
            if (nextOp != null) {
                // Queued op found; execute it
                Minecraft.getInstance().execute(nextOp);
            } else {
                // No queued ops; lower flag
                ClientSort.operatingClient = false;
            }
            return InteractionManager.TICK_WAITER;
        });
    }

    public static boolean sort(
            AbstractContainerScreen<?> screen,
            Slot originSlot,
            boolean onlyClient,
            SortOrder sortOrder
    ) {
        if (sortOrder.equals(SortOrder.NONE))
            return false;

        return operate(
                screen,
                originSlot,
                onlyClient,
                Operation.SORT,
                (op) -> op.sort(sortOrder)
        );
    }

    public static boolean fillStacks(
            AbstractContainerScreen<?> screen,
            Slot originSlot,
            boolean onlyClient
    ) {
        return operate(
                screen,
                originSlot,
                onlyClient,
                Operation.STACK_FILL,
                SingleUseOperator::fillStacks
        );
    }

    public static boolean transferMatching(
            AbstractContainerScreen<?> screen,
            Slot originSlot,
            boolean onlyClient
    ) {
        return operate(
                screen,
                originSlot,
                onlyClient,
                Operation.MATCH_TRANSFER,
                SingleUseOperator::matchTransfer
        );
    }

    public static boolean transfer(
            AbstractContainerScreen<?> screen,
            Slot originSlot,
            boolean onlyClient
    ) {
        return operate(
                screen,
                originSlot,
                onlyClient,
                Operation.TRANSFER,
                SingleUseOperator::transfer
        );
    }

    /**
     * Creates an instance of {@link SingleUseOperator} optimized for the current game state and
     * valid for a single operation of the specified type, and uses it to perform the operation,
     * unless the operation is disallowed by policy.
     *
     * @return {@code true} if the operation was performed or enqueued.
     */
    private static boolean operate(
            AbstractContainerScreen<?> screen,
            Slot originSlot,
            boolean onlyClient,
            Operation operation,
            Consumer<SingleUseOperator> wrapper
    ) {
        // No-op in spectator mode
        if (Minecraft.getInstance().player.isSpectator())
            return false;

        // Check policy
        Object object = getObj(originSlot, screen.getMenu());
        if (object == null)
            return false;
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
                return false;
            }
        }

        // Preference server-accelerated ops
        if (!onlyClient && options().useServerAcceleration
                && ClientPlatformServices.getInstance().canSendToServer(operation.id)) {
            if (debug())
                ClientSort.LOG.info("Preparing server operator for {}", operation.name());
            wrapper.accept(new ServerOperator(screen, originSlot, operation));
            return true;
        }

        // Fall back to client ops
        Runnable op = () -> {
            if (debug())
                ClientSort.LOG.info("Preparing server operator for {}", operation.name());
            wrapper.accept(getClientOperator(screen, originSlot, operation));
            endClientOp();
        };
        if (ClientSort.operatingClient) {
            // A client-side op is already in progress, add this op to the queue
            if (ClientSort.clientOpQueue.offer(op)) {
                if (debug())
                    ClientSort.LOG.warn(
                            "Client operation added to queue: another operation is in progress!"
                    );
                return true;
            } else {
                if (debug())
                    ClientSort.LOG.warn(
                            "Client operation rejected: another operation is in progress and the queue is full!"
                    );
                return false;
            }
        } else {
            startClientOp();
            op.run();
            return true;
        }
    }

    private static SingleUseOperator getClientOperator(
            AbstractContainerScreen<?> screen,
            Slot originSlot,
            Operation operation
    ) {
        if (Objects.requireNonNull(Minecraft.getInstance().player).isCreative()
                && screen instanceof CreativeModeInventoryScreen) {
            if (debug())
                ClientSort.LOG.info("Preparing client-creative operator for {}", operation.name());
            return new ClientCreativeOperator(screen, originSlot, operation);
        } else {
            if (debug())
                ClientSort.LOG.info("Preparing client-survival operator for {}", operation.name());
            return new ClientSurvivalOperator(screen, originSlot, operation);
        }
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
