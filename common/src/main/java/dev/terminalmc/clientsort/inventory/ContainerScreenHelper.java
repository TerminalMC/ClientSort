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

package dev.terminalmc.clientsort.inventory;

import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.network.ClickEventFactory;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerScreenHelper<T extends AbstractContainerScreen<?>> {
    protected final T screen;
    protected final ClickEventFactory clickEventFactory;
    public static final int INVALID_SCOPE = Integer.MAX_VALUE;

    protected ContainerScreenHelper(T screen, ClickEventFactory clickEventFactory) {
        this.screen = screen;
        this.clickEventFactory = clickEventFactory;
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractContainerScreen<?>> ContainerScreenHelper<T> of(T screen, ClickEventFactory clickEventFactory) {
        if (screen instanceof CreativeModeInventoryScreen) {
            return (ContainerScreenHelper<T>) new CreativeContainerScreenHelper<>((CreativeModeInventoryScreen) screen, clickEventFactory);
        }
        return new ContainerScreenHelper<>(screen, clickEventFactory);
    }

    public InteractionManager.InteractionEvent createClickEvent(Slot slot, int action, ClickType actionType) {
        return clickEventFactory.create(slot, action, actionType);
    }

    public boolean isHotbarSlot(Slot slot) {
        return ((ISlot) slot).mouseWheelie_getIndexInInv() < 9;
    }

    public int getScope(Slot slot) {
        return getScope(slot, false);
    }

    public int getScope(Slot slot, boolean preferSmallerScopes) {
        if (!slot.mayPlace(ItemStack.EMPTY)) {
            // Removed checks:
            // slot.container == null 
            // (always false)
            // ((ISlot) slot).mouseWheelie_getIndexInInv() >= slot.container.getContainerSize()
            // (prevents compatibility with Traveler's Backpack)
            return INVALID_SCOPE;
        }
        if (screen instanceof AbstractContainerScreen) {
            if (slot.container instanceof Inventory) {
                Config.Options options = Config.options();
                if (isHotbarSlot(slot)) {
                    if (options.hotbarMode == Config.Options.HotbarMode.HARD
                            || options.hotbarMode == Config.Options.HotbarMode.SOFT && preferSmallerScopes) {
                        return -1;
                    }
                } else if (((ISlot) slot).mouseWheelie_getIndexInInv() >= 40) {
                    if (options.extraSlotMode == Config.Options.ExtraSlotMode.NONE) {
                        return -2;
                    } else if (options.extraSlotMode == Config.Options.ExtraSlotMode.HOTBAR 
                            && (options.hotbarMode == Config.Options.HotbarMode.HARD 
                            || options.hotbarMode == Config.Options.HotbarMode.SOFT && preferSmallerScopes)) {
                        return -1;
                    }
                }
                return 0;
            } else {
                return 2;
            }
        } else {
            if (slot.container instanceof Inventory) {
                if (isHotbarSlot(slot)) {
                    Config.Options options = Config.options();
                    if (options.hotbarMode == Config.Options.HotbarMode.HARD
                            || options.hotbarMode == Config.Options.HotbarMode.SOFT && preferSmallerScopes) {
                        return -1;
                    }
                }
                return 0;
            }
            return 1;
        }
    }
}
