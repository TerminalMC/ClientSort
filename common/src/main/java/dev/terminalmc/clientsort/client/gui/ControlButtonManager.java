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

    // Alters whether buttons are arrayed horizontally, vertically or :/
    private static final int BUTTON_SHIFT_X = 0;
    private static final int BUTTON_SHIFT_Y = 1;

    private static final LinkedHashSet<ControlButton> containerButtons = new LinkedHashSet<>();
    private static final LinkedHashSet<ControlButton> playerButtons = new LinkedHashSet<>();

    public static LinkedList<ControlButton> getContainerButtons() {
        return new LinkedList<>(containerButtons);
    }

    public static LinkedList<ControlButton> getPlayerButtons() {
        return new LinkedList<>(playerButtons);
    }

    /**
     * Generates zero or more control buttons in accordance with config and
     * state, and if any were generated, adds them to the screen.
     */
    public static void afterScreenInit(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> acs)) return;
        if (!options().showButtons) return;

        containerButtons.clear();
        playerButtons.clear();

        // Allow forcing buttons to be shown on editor screens
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

        // Generate container-side buttons
        generate(acs, false, forceShowContainer, options().firstButton);
        generate(acs, false, forceShowContainer, options().secondButton);
        generate(acs, false, forceShowContainer, options().thirdButton);

        // Generate player-side buttons
        generate(acs, true, forceShowPlayer, options().firstButton);
        generate(acs, true, forceShowPlayer, options().secondButton);
        generate(acs, true, forceShowPlayer, options().thirdButton);
    }

    /**
     * Generates zero or one config buttons in accordance with params and
     * config, and if a button was generated, adds it to the screen.
     */
    private static void generate(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShow,
            Config.Options.CONTROL_BUTTON type
    ) {
        switch(type) {
            case SORT -> generateSortButton(screen, isPlayerInv, forceShow);
            case STACK_FILL -> generateStackFillButton(screen, isPlayerInv, forceShow);
            case TRANSFER -> generateTransferButton(screen, isPlayerInv, forceShow);
        }
    }

    private static void generateSortButton(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Sanity check; we need a player to work with
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Preliminary check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv) return;

        // Preliminary check; never display buttons on basic workstations
        // or other minor inventories
        if (getNumberOfBulkInventorySlots(screen, isPlayerInv) < 3) return;

        // Get the reference or positional anchor slot
        Slot referenceSlot = getReferenceSlot(screen, isPlayerInv);
        if (referenceSlot == null) return;

        // Get the container we're adding buttons for
        Container container = isPlayerInv ? player.getInventory() : getContainer(player);
        if (container == null) return;

        // Retrieve the relevant container or GUI class
        Object object = container instanceof SimpleContainer ? screen.getMenu() : container;
        // Check for perfect layout match
        ButtonLayout layout = options().buttonLayouts.get(object.getClass().getName());
        // If no perfect match, try to find a layout for any superclass
        // TODO benchmark and if it's slow, consider caching
        // TODO depending on benchmark, consider performing inheritance cross-
        //  checks of instance-matching layouts to ensure that we always get
        //  the closest one
        if (layout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(object, l.className)) {
                    layout = l;
                    break;
                }
            }
        }

        // Get the configured or default offset
        Vec2i offset;
        if (layout != null) {
            if (layout.offset != null) {
                offset = layout.offset;
            } else {
                offset = options().buttonDefaultOffset;
            }
        } else {
            offset = options().buttonDefaultOffset;
        }

        // Only add the button if it's whitelisted or forced, and if it's
        // forced, show it as inactive
        boolean active;
        if (layout != null && layout.sortEnabled) {
            active = true;
        } else if (forceShow) {
            active = false;
        } else {
            return;
        }

        // Create and add
        SortButton button = new SortButton(
                screen,
                container,
                layout != null ? layout.className : container.getClass().getName(),
                isPlayerInv,
                referenceSlot,
                getShiftedOffset(offset, isPlayerInv),
                active
        );
        addButton(screen, button, isPlayerInv);
    }

    private static void generateStackFillButton(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Sanity check; we need a player to work with
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Preliminary check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv) return;

        // Preliminary check; never display buttons on basic workstations
        // or other minor inventories
        if (getNumberOfBulkInventorySlots(screen, false) < 3) return;

        // Get the reference or positional anchor slot
        Slot referenceSlot = getReferenceSlot(screen, isPlayerInv);
        if (referenceSlot == null) return;

        // Get the container we're adding buttons for, and the other container
        Container srcContainer = isPlayerInv ? player.getInventory() : getContainer(player);
        Container dstContainer = isPlayerInv ? getContainer(player) : player.getInventory();
        if (srcContainer == null || dstContainer == null || srcContainer == dstContainer) return;

        // Retrieve the relevant container or GUI class
        Object srcObject = srcContainer instanceof SimpleContainer ? screen.getMenu() : srcContainer;
        // Check for perfect layout match
        ButtonLayout srcLayout = options().buttonLayouts.get(srcObject.getClass().getName());
        // If no perfect match, try to find a layout for any superclass
        if (srcLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(srcObject, l.className)) {
                    srcLayout = l;
                    break;
                }
            }
        }
        // Retrieve the relevant container or GUI class
        Object dstObject = dstContainer instanceof SimpleContainer ? screen.getMenu() : dstContainer;
        // Check for perfect layout match
        ButtonLayout dstLayout = options().buttonLayouts.get(dstObject.getClass().getName());
        // If no perfect match, try to find a layout for any superclass
        if (dstLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(dstObject, l.className)) {
                    dstLayout = l;
                    break;
                }
            }
        }

        // Get the configured or default offset
        Vec2i offset;
        if (srcLayout != null) {
            if (srcLayout.offset != null) {
                offset = srcLayout.offset;
            } else {
                offset = options().buttonDefaultOffset;
            }
        } else {
            offset = options().buttonDefaultOffset;
        }

        // Only add the button if it's whitelisted or forced, and if it's
        // forced, show it as inactive
        boolean active;
        if (srcLayout != null && srcLayout.stackFillEnabled
                && dstLayout != null && dstLayout.stackFillEnabled
        ) {
            active = true;
        } else if (forceShow) {
            active = false;
        } else {
            return;
        }

        // Create and add
        StackFillButton button = new StackFillButton(
                screen,
                srcContainer,
                srcLayout != null ? srcLayout.className : srcContainer.getClass().getName(),
                isPlayerInv,
                referenceSlot,
                getShiftedOffset(offset, isPlayerInv),
                active
        );
        addButton(screen, button, isPlayerInv);
    }

    private static void generateTransferButton(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Sanity check; we need a player to work with
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // Preliminary check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv) return;

        // Preliminary check; never display buttons on basic workstations
        // or other minor inventories
        if (getNumberOfBulkInventorySlots(screen, false) < 3) return;

        // Get the reference or positional anchor slot
        Slot referenceSlot = getReferenceSlot(screen, isPlayerInv);
        if (referenceSlot == null) return;

        // Get the container we're adding buttons for, and the other container
        Container srcContainer = isPlayerInv ? player.getInventory() : getContainer(player);
        Container dstContainer = isPlayerInv ? getContainer(player) : player.getInventory();
        if (srcContainer == null || dstContainer == null || srcContainer == dstContainer) return;

        // Retrieve the relevant container or GUI class
        Object srcObject = srcContainer instanceof SimpleContainer ? screen.getMenu() : srcContainer;
        // Check for perfect layout match
        ButtonLayout srcLayout = options().buttonLayouts.get(srcObject.getClass().getName());
        // If no perfect match, try to find a layout for any superclass
        if (srcLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(srcObject, l.className)) {
                    srcLayout = l;
                    break;
                }
            }
        }
        // Retrieve the relevant container or GUI class
        Object dstObject = dstContainer instanceof SimpleContainer ? screen.getMenu() : dstContainer;
        // Check for perfect layout match
        ButtonLayout dstLayout = options().buttonLayouts.get(dstObject.getClass().getName());
        // If no perfect match, try to find a layout for any superclass
        if (dstLayout == null) {
            for (ButtonLayout l : options().buttonLayouts.values()) {
                if (isInstanceOf(dstObject, l.className)) {
                    dstLayout = l;
                    break;
                }
            }
        }

        // Get the configured or default offset
        Vec2i offset;
        if (srcLayout != null) {
            if (srcLayout.offset != null) {
                offset = srcLayout.offset;
            } else {
                offset = options().buttonDefaultOffset;
            }
        } else {
            offset = options().buttonDefaultOffset;
        }

        // Only add the button if it's whitelisted or forced, and if it's
        // forced, show it as inactive
        boolean active;
        if (srcLayout != null && srcLayout.transferEnabled
                && dstLayout != null && dstLayout.transferEnabled
        ) {
            active = true;
        } else if (forceShow) {
            active = false;
        } else {
            return;
        }

        // Create and add
        TransferButton button = new TransferButton(
                screen,
                srcContainer,
                srcLayout != null ? srcLayout.className : srcContainer.getClass().getName(),
                isPlayerInv,
                referenceSlot,
                getShiftedOffset(offset, isPlayerInv),
                active
        );
        addButton(screen, button, isPlayerInv);
    }

    /**
     * @return {@code true} if the class name represents a valid and loadable
     * class of which the object is an instance.
     */
    public static boolean isInstanceOf(Object object, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.isInstance(object);
        } catch (ClassNotFoundException e) {
            if (ClientSort.debug) {
                ClientSort.LOG.warn("Unable to check instance for object '{}': Class '{}' not found.",
                        object.getClass().getName(), className);
            }
            return false;
        }
    }

    /**
     * @return the container associated with the player's container menu, if it
     * exists.
     */
    public static @Nullable Container getContainer(Player player) {
        try {
            return player.containerMenu.getSlot(0).container;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Adds the button to the screen, and to the respective set.
     */
    private static void addButton(
            AbstractContainerScreen<?> screen,
            ControlButton button,
            boolean isPlayerInv
    ) {
        ((ScreenAccessor)screen).callAddRenderableWidget(button);
        (isPlayerInv ? playerButtons : containerButtons).add(button);
    }

    /**
     * @return the slot to which a button position in the respective container
     * should be anchored, if any are available.
     */
    private static @Nullable Slot getReferenceSlot(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv
    ) {
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInv == (slot.container instanceof Inventory))
                .max(Comparator.comparingInt(slot -> slot.x - slot.y))
                .orElse(null);
    }

    /**
     * @return the number of slots in the respective container that are
     * theoretically able to store any item.
     */
    private static int getNumberOfBulkInventorySlots(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv
    ) {
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInv == (slot.container instanceof Inventory))
                .filter(slot -> !(screen.getMenu() instanceof HorseInventoryMenu)
                        || slot.getContainerSlot() >= 2)
                .mapToInt(slot -> 1)
                .sum();
    }

    /**
     * @return the offset, shifted by a constant amount based on the number
     * of buttons already generated.
     */
    @SuppressWarnings("ConstantValue")
    public static Vec2i getShiftedOffset(Vec2i offset, boolean isPlayerInv) {
        int index = (isPlayerInv ? playerButtons : containerButtons).size();

        int x = offset.x() + BUTTON_SHIFT_X * (ControlButton.WIDTH + BUTTON_SPACING) * index;
        int y = offset.y() + BUTTON_SHIFT_Y * (ControlButton.HEIGHT + BUTTON_SPACING) * index;

        return new Vec2i(x, y);
    }
}
