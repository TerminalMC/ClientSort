/*
 * Copyright 2021 Evan Steinkerchner (Roundaround)
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

package dev.terminalmc.clientsort.client.gui;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.config.ButtonLayout;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.gui.screen.edit.GroupSelectorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.ContainerPositionEditScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.PlayerPositionEditScreen;
import dev.terminalmc.clientsort.client.gui.widget.ControlButton;
import dev.terminalmc.clientsort.client.gui.widget.SortButton;
import dev.terminalmc.clientsort.client.gui.widget.StackFillButton;
import dev.terminalmc.clientsort.client.gui.widget.TransferButton;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.mixin.client.accessor.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import static dev.terminalmc.clientsort.client.config.Config.options;

public class ControlButtonManager {
    private ControlButtonManager() {}

    private static final int BUTTON_SPACING = 1;

    // Alters whether buttons are arrayed horizontally or vertically
    private static final int BUTTON_SHIFT_X = 0;
    private static final int BUTTON_SHIFT_Y = 1;

    private static final LinkedHashSet<ControlButton> containerButtons = new LinkedHashSet<>();
    private static final LinkedHashSet<ControlButton> playerButtons = new LinkedHashSet<>();

    public static void afterScreenInit(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> acs)) return;
        if (!options().showButtons) return;

        containerButtons.clear();
        playerButtons.clear();

        ClientSort.LOG.warn("asi {}", screen.getClass().getSimpleName());

        boolean forceShowContainer = false;
        boolean forceShowPlayer = false;

        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof GroupSelectorScreen) {
            forceShowContainer = true;
            forceShowPlayer = true;
        } else if (currentScreen instanceof ContainerPositionEditScreen) {
            forceShowContainer = true;
        } else if (currentScreen instanceof PlayerPositionEditScreen) {
            forceShowPlayer = true;
        }

        // Container side
        generate(acs, false, forceShowContainer, forceShowPlayer, options().firstButton);
        generate(acs, false, forceShowContainer, forceShowPlayer, options().secondButton);
        generate(acs, false, forceShowContainer, forceShowPlayer, options().thirdButton);

        // Player side
        generate(acs, true, forceShowContainer, forceShowPlayer, options().firstButton);
        generate(acs, true, forceShowContainer, forceShowPlayer, options().secondButton);
        generate(acs, true, forceShowContainer, forceShowPlayer, options().thirdButton);
    }

    private static void generate(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShowContainer,
            boolean forceShowPlayer,
            Config.Options.CONTROL_BUTTON type
    ) {
        boolean forceShow = isPlayerInv ? forceShowPlayer : forceShowContainer;
        switch(type) {
            case SORT -> generateSortButton(screen, isPlayerInv, forceShow);
            case STACK_FILL -> generateStackFillButton(screen, isPlayerInv, forceShow);
            case TRANSFER -> generateTransferButton(screen, isPlayerInv, forceShow);
        }
    }

    public static boolean isInstanceOf(Object obj, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.isInstance(obj);
        } catch (ClassNotFoundException e) {
            if (ClientSort.debug) {
                ClientSort.LOG.warn("Unable to check instance for object '{}': Class '{}' not found.",
                        obj.getClass().getName(), className);
            }
            return false;
        }
    }

    public static @Nullable Container getContainer(Player player) {
        try {
            return player.containerMenu.getSlot(0).container;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static void generateSortButton(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Do not show container buttons on player inventory screen
        if (screen instanceof InventoryScreen && !isPlayerInv) return;

        if (getNumberOfBulkInventorySlots(screen, isPlayerInv) < 3) return;

        Slot referenceSlot = getReferenceSlot(screen, isPlayerInv);
        if (referenceSlot == null) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Get the relevant container
        Container container = isPlayerInv ? player.getInventory() : getContainer(player);
        if (container == null) return;

        boolean enabled;
        Vec2i offset;

        // Retrieve the layout data
        // TODO consider caching here?
        Object object = container instanceof SimpleContainer ? screen.getMenu() : container;
        ButtonLayout layout = options().buttonLayouts.get(object.getClass().getName());
        if (layout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(object, l.className)) {
                    layout = l;
                    break;
                }
            }
        }

        // Only show buttons if forced or whitelisted
        if (layout != null) {
            enabled = layout.sortEnabled;
            offset = layout.offset != null ? layout.offset : options().buttonDefaultOffset;
        } else if (forceShow) {
            enabled = false;
            offset = options().buttonDefaultOffset;
        } else {
            return;
        }

        if (!enabled && !forceShow) return;

        Vec2i awareOffset = getButtonPosition(
                (isPlayerInv ? playerButtons : containerButtons).size(),
                offset
        );
        SortButton button = new SortButton(
                screen,
                container,
                layout != null ? layout.className : container.getClass().getName(),
                isPlayerInv,
                referenceSlot,
                awareOffset,
                enabled
        );
        addButton(screen, button, isPlayerInv);
    }

    private static void generateStackFillButton(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Do not show container buttons on player inventory screen
        if (screen instanceof InventoryScreen && !isPlayerInv) return;

        if (getNumberOfNonPlayerBulkInventorySlots(screen) < 3) return;

        Slot referenceSlot = getReferenceSlot(screen, isPlayerInv);
        if (referenceSlot == null) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Get the relevant containers
        Container srcContainer = isPlayerInv ? getContainer(player) : player.getInventory();
        Container dstContainer = isPlayerInv ? player.getInventory() : getContainer(player);
        if (srcContainer == null || dstContainer == null || srcContainer == dstContainer) return;

        boolean enabled;
        Vec2i offset;

        // Retrieve the layout data
        Object srcObject = srcContainer instanceof SimpleContainer ? screen.getMenu() : srcContainer;
        ButtonLayout srcLayout = options().buttonLayouts.get(srcObject.getClass().getName());
        if (srcLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(srcObject, l.className)) {
                    srcLayout = l;
                    break;
                }
            }
        }
        Object dstObject = dstContainer instanceof SimpleContainer ? screen.getMenu() : dstContainer;
        ButtonLayout dstLayout = options().buttonLayouts.get(dstObject.getClass().getName());
        if (dstLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(dstObject, l.className)) {
                    dstLayout = l;
                    break;
                }
            }
        }

        // Only show buttons if forced or whitelisted
        if (srcLayout != null && dstLayout != null && dstLayout.stackFillEnabled) {
            enabled = srcLayout.stackFillEnabled;
            offset = srcLayout.offset != null ? srcLayout.offset : options().buttonDefaultOffset;
        } else if (forceShow) {
            enabled = false;
            offset = options().buttonDefaultOffset;
        } else {
            return;
        }

        if (!enabled && !forceShow) return;

        Vec2i awareOffset = getButtonPosition(
                (isPlayerInv ? playerButtons : containerButtons).size(),
                offset
        );
        StackFillButton button = new StackFillButton(
                screen,
                srcContainer,
                srcLayout != null ? srcLayout.className : srcContainer.getClass().getName(),
                isPlayerInv,
                referenceSlot,
                awareOffset,
                enabled
        );
        addButton(screen, button, isPlayerInv);
    }

    private static void generateTransferButton(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Do not show container buttons on player inventory screen
        if (screen instanceof InventoryScreen && !isPlayerInv) return;

        if (getNumberOfNonPlayerBulkInventorySlots(screen) < 3) return;

        Slot referenceSlot = getReferenceSlot(screen, isPlayerInv);
        if (referenceSlot == null) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Get the relevant containers
        Container srcContainer = isPlayerInv ? getContainer(player) : player.getInventory();
        Container dstContainer = isPlayerInv ? player.getInventory() : getContainer(player);
        if (srcContainer == null || dstContainer == null || srcContainer == dstContainer) return;

        boolean enabled;
        Vec2i offset;

        // Retrieve the layout data
        Object srcObject = srcContainer instanceof SimpleContainer ? screen.getMenu() : srcContainer;
        ButtonLayout srcLayout = options().buttonLayouts.get(srcObject.getClass().getName());
        if (srcLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(srcObject, l.className)) {
                    srcLayout = l;
                    break;
                }
            }
        }
        Object dstObject = dstContainer instanceof SimpleContainer ? screen.getMenu() : dstContainer;
        ButtonLayout dstLayout = options().buttonLayouts.get(dstObject.getClass().getName());
        if (dstLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(dstObject, l.className)) {
                    dstLayout = l;
                    break;
                }
            }
        }

        // Only show buttons if forced or whitelisted
        if (srcLayout != null && dstLayout != null && dstLayout.transferEnabled) {
            enabled = srcLayout.transferEnabled;
            offset = srcLayout.offset != null ? srcLayout.offset : options().buttonDefaultOffset;
        } else if (forceShow) {
            enabled = false;
            offset = options().buttonDefaultOffset;
        } else {
            return;
        }

        if (!enabled && !forceShow) return;

        Vec2i awareOffset = getButtonPosition(
                (isPlayerInv ? playerButtons : containerButtons).size(),
                offset
        );
        TransferButton button = new TransferButton(
                screen,
                srcContainer,
                srcLayout != null ? srcLayout.className : srcContainer.getClass().getName(),
                isPlayerInv,
                referenceSlot,
                awareOffset,
                enabled
        );
        addButton(screen, button, isPlayerInv);
    }

    private static void addButton(
            AbstractContainerScreen<?> screen,
            ControlButton button,
            boolean isPlayerInv
    ) {
        ((ScreenAccessor)screen).callAddRenderableWidget(button);
        (isPlayerInv ? playerButtons : containerButtons).add(button);
    }

    private static @Nullable Slot getReferenceSlot(AbstractContainerScreen<?> screen, boolean isPlayerInv) {
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInv == (slot.container instanceof Inventory))
                .max(Comparator.comparingInt(slot -> slot.x - slot.y))
                .orElse(null);
    }

    private static int getNumberOfBulkInventorySlots(AbstractContainerScreen<?> screen, boolean isPlayerInv) {
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInv == (slot.container instanceof Inventory))
                .filter(slot -> !(screen.getMenu() instanceof HorseInventoryMenu) || slot.getContainerSlot() >= 2)
                .mapToInt(slot -> 1)
                .sum();
    }

    private static int getNumberOfNonPlayerBulkInventorySlots(AbstractContainerScreen<?> screen) {
        return screen.getMenu().slots.stream()
                .filter(slot -> !(slot.container instanceof Inventory))
                .filter(slot -> !(screen.getMenu() instanceof HorseInventoryMenu) || slot.getContainerSlot() >= 2)
                .mapToInt(slot -> 1)
                .sum();
    }

    @SuppressWarnings("ConstantValue")
    public static Vec2i getButtonPosition(int index, Vec2i offset) {
        int x = offset.x() + BUTTON_SHIFT_X * (ControlButton.WIDTH + BUTTON_SPACING) * index;
        int y = offset.y() + BUTTON_SHIFT_Y * (ControlButton.HEIGHT + BUTTON_SPACING) * index;

        return new Vec2i(x, y);
    }

    public static LinkedList<ControlButton> getContainerButtons() {
        return new LinkedList<>(containerButtons);
    }

    public static LinkedList<ControlButton> getPlayerButtons() {
        return new LinkedList<>(playerButtons);
    }
}
