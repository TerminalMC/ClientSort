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

package dev.terminalmc.clientsort.client.inventory.screen;

import dev.terminalmc.clientsort.client.inventory.util.Scope;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.util.SoundManager;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Provides slot scope information and interaction methods for an instance of
 * {@link AbstractContainerScreen}.
 */
public class ContainerScreenHelper<T extends AbstractContainerScreen<?>> {

    protected final T screen;
    protected final InteractionManager.ClickEventFactory clickEventFactory;

    protected ContainerScreenHelper(
            T screen,
            InteractionManager.ClickEventFactory clickEventFactory
    ) {
        this.screen = screen;
        this.clickEventFactory = clickEventFactory;
    }

    /**
     * Creates a {@link ContainerScreenHelper} for {@code screen}.
     */
    public static <T extends AbstractContainerScreen<?>> ContainerScreenHelper<T> of(
            T screen,
            InteractionManager.ClickEventFactory clickEventFactory
    ) {
        // Creative inventory screen helper
        if (screen instanceof CreativeModeInventoryScreen) {
            //noinspection unchecked
            return (ContainerScreenHelper<T>) new CreativeContainerScreenHelper<>(
                    (CreativeModeInventoryScreen) screen,
                    clickEventFactory
            );
        }
        // Normal helper
        return new ContainerScreenHelper<>(screen, clickEventFactory);
    }

    /**
     * Creates a {@link ContainerScreenHelper} for {@code screen}.
     */
    public static <T extends AbstractContainerScreen<?>> ContainerScreenHelper<T> of(T screen) {
        InteractionManager.ClickEventFactory clickEventFactory =
                (slot, mouseButton, clickType, playSound) -> new InteractionManager.CallbackEvent(() -> {
                    ((AbstractContainerScreenAccessor) screen).clientsort$slotClicked(
                            slot,
                            ((ISlot) slot).clientsort$getIdInContainer(),
                            mouseButton,
                            clickType
                    );
                    if (playSound)
                        SoundManager.play();
                    return InteractionManager.TICK_WAITER;
                });
        return of(screen, clickEventFactory);
    }

    /**
     * Creates a click event in the {@link AbstractContainerScreen}.
     */
    public InteractionManager.InteractionEvent createClickEvent(
            Slot slot,
            int button,
            ClickType clickType,
            boolean playSound
    ) {
        return clickEventFactory.create(slot, button, clickType, playSound);
    }

    /**
     * @return {@code true} if the index of the slot in its inventory is less than 9.
     */
    public boolean isHotbarSlot(Slot slot) {
        return ((ISlot) slot).clientsort$getIndexInInv() < 9;
    }

    /**
     * @return {@code true} if the index of the slot in its inventory is greater than 35.
     */
    public boolean isExtraSlot(Slot slot) {
        return ((ISlot) slot).clientsort$getIndexInInv() > 35;
    }

    /**
     * Gets the scope of the specified {@link Slot}.
     * <p>
     * Scope is a heuristic system for grouping slots together based on their location in the
     * inventory.
     *
     * @param slot the slot for which to get the scope.
     * @return the scope of the slot, or {@link Scope#INVALID} if the slot is not accessible.
     */
    public Scope getScope(Slot slot) {
        // Screen with only player inventory
        if (screen instanceof EffectRenderingInventoryScreen) {
            // Player inventory
            if (slot.container instanceof Inventory) {
                boolean mergeWithHotbar = false;

                // Extra inventory slots e.g. offhand
                if (isExtraSlot(slot)) {
                    switch (options().extraSlotScope) {
                        case HOTBAR -> mergeWithHotbar = true;
                        case EXTRA -> {
                            return Scope.PLAYER_INV_EXTRA;
                        }
                        case NONE -> {
                            return Scope.INVALID;
                        }
                    }
                }

                // Hotbar
                if (mergeWithHotbar || isHotbarSlot(slot)) {
                    switch (options().hotbarScope) {
                        case HOTBAR -> {
                            return Scope.PLAYER_INV_HOTBAR;
                        }
                        case NONE -> {
                            return Scope.INVALID;
                        }
                    }
                }

                return Scope.PLAYER_INV;
            }

            // Out of inventory e.g. 2x2 crafting grid
            else {
                return Scope.PLAYER_OTHER;
            }
        }

        // Screen with container, and probably player inventory attached
        else {
            // Player inventory
            if (slot.container instanceof Inventory) {
                // Hotbar
                if (isHotbarSlot(slot)) {
                    switch (options().hotbarScope) {
                        case HOTBAR -> {
                            return Scope.PLAYER_INV_HOTBAR;
                        }
                        case NONE -> {
                            return Scope.INVALID;
                        }
                    }
                }

                return Scope.PLAYER_INV;
            }

            // Container
            else {
                return Scope.CONTAINER_INV;
            }
        }
    }

    /**
     * Workaround for inconsistency between client-side and server-side inventory sizes (and
     * therefore slot indexes).
     */
    public void translateSlotIds(int[] slotMapping) {
    }
}
