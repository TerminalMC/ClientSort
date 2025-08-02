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

import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.gui.screen.edit.ContainerEditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.PlayerEditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.SelectorScreen;
import dev.terminalmc.clientsort.client.gui.widget.SortButton;
import dev.terminalmc.clientsort.client.gui.widget.StackFillButton;
import dev.terminalmc.clientsort.client.gui.widget.TransferButton;
import dev.terminalmc.clientsort.client.gui.widget.TriggerButton;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.util.Scope;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import dev.terminalmc.clientsort.mixin.client.accessor.ScreenAccessor;
import dev.terminalmc.clientsort.util.inject.ISlot;
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

public class TriggerButtonManager {

    private TriggerButtonManager() {
    }

    private static final int BUTTON_SPACING = 1;

    // Alters whether buttons are arrayed horizontally, vertically or :/
    private static final int BUTTON_SHIFT_X = 0;
    private static final int BUTTON_SHIFT_Y = 1;

    private static final LinkedHashSet<TriggerButton> containerButtons = new LinkedHashSet<>();
    private static final LinkedHashSet<TriggerButton> playerButtons = new LinkedHashSet<>();

    public static LinkedList<TriggerButton> getContainerButtons() {
        return new LinkedList<>(containerButtons);
    }

    public static LinkedList<TriggerButton> getPlayerButtons() {
        return new LinkedList<>(playerButtons);
    }

    /**
     * Generates zero or more control buttons in accordance with config and state, and if any were
     * generated, adds them to the screen.
     */
    public static void afterScreenInit(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> acs))
            return;

        containerButtons.clear();
        playerButtons.clear();

        if (!options().showButtons)
            return;

        // Allow forcing buttons to be shown on editor screens
        boolean forceShowContainer = false;
        boolean forceShowPlayer = false;
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof SelectorScreen) {
            forceShowContainer = true;
            forceShowPlayer = true;
        } else if (currentScreen instanceof ContainerEditorScreen) {
            forceShowContainer = true;
        } else if (currentScreen instanceof PlayerEditorScreen) {
            forceShowPlayer = true;
        }

        // Generate container-side buttons
        Slot containerRefSlot = getReferenceSlot(acs, false);
        if (containerRefSlot != null) {
            generate(acs, containerRefSlot, false, forceShowContainer, options().firstButtonOp);
            generate(acs, containerRefSlot, false, forceShowContainer, options().secondButtonOp);
            generate(acs, containerRefSlot, false, forceShowContainer, options().thirdButtonOp);
        }

        // Generate player-side buttons
        Slot playerRefSlot = getReferenceSlot(acs, true);
        if (playerRefSlot != null) {
            generate(acs, playerRefSlot, true, forceShowPlayer, options().firstButtonOp);
            generate(acs, playerRefSlot, true, forceShowPlayer, options().secondButtonOp);
            generate(acs, playerRefSlot, true, forceShowPlayer, options().thirdButtonOp);
        }
    }

    /**
     * Generates zero or one config buttons in accordance with params and config, and if a button
     * was generated, adds it to the screen.
     */
    private static void generate(
            AbstractContainerScreen<?> screen,
            Slot refSlot,
            boolean isPlayerInv,
            boolean forceShow,
            Operation op
    ) {
        switch (op) {
            case SORT -> generateSortButton(screen, refSlot, isPlayerInv, forceShow);
            case STACK_FILL -> generateStackFillButton(screen, refSlot, isPlayerInv, forceShow);
            case TRANSFER -> generateTransferButton(screen, refSlot, isPlayerInv, forceShow);
        }
    }

    private static void generateSortButton(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Sanity check; we need a player to work with
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        // Sanity check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv)
            return;

        // Preliminary check; never display buttons on basic workstations or other minor inventories
        if (getNumberOfBulkInventorySlots(screen, isPlayerInv) < 3)
            return;

        // Get the relevant container, if any
        @Nullable Container container = isPlayerInv
                ? player.getInventory()
                : getContainer(player);
        // Sanity check; we need a container to work with
        if (container == null)
            return;

        // Select the relevant container or GUI class
        Object object = container instanceof SimpleContainer
                ? screen.getMenu()
                : container;

        // Retrieve the relevant policy, if any
        @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());
        if ((policy == null || !policy.showSortButton()) && !forceShow)
            return;

        // Get the configured or default offset
        Vec2i offset = policy != null
                ? policy.getButtonOffset()
                : options().layoutOffset;

        // Create and add
        SortButton button = new SortButton(
                screen,
                container,
                referenceSlot,
                isPlayerInv,
                policy,
                object.getClass().getName(),
                getShiftedOffset(offset, isPlayerInv)
        );
        addButton(screen, button, isPlayerInv);
    }

    private static void generateStackFillButton(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Sanity check; we need a player to work with
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        // Sanity check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv)
            return;

        // Preliminary check; never display buttons on basic workstations or other minor inventories
        if (getNumberOfBulkInventorySlots(screen, isPlayerInv) < 3)
            return;

        // Get the relevant container, if any
        @Nullable Container container = isPlayerInv
                ? player.getInventory()
                : getContainer(player);
        // Sanity check; we need a container to work with
        if (container == null)
            return;

        // Select the relevant container or GUI class
        Object object = container instanceof SimpleContainer
                ? screen.getMenu()
                : container;

        // Check the relevant policy, if any
        @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());
        if ((policy == null || !policy.showStackFillButton()) && !forceShow)
            return;

        // Get the configured or default offset
        Vec2i offset = policy != null
                ? policy.getButtonOffset()
                : options().layoutOffset;

        // Get the destination container, if any
        @Nullable Container dstContainer = isPlayerInv
                ? getContainer(player)
                : player.getInventory();
        if (dstContainer != null) {
            // Select the relevant container or GUI class
            Object dstObject = dstContainer instanceof SimpleContainer
                    ? screen.getMenu()
                    : dstContainer;

            // Check the relevant policy, if any
            @Nullable ClassPolicy dstPolicy = PolicyManager.getPolicy(dstObject.getClass());
            if ((dstPolicy == null || !dstPolicy.showStackFillButton()) && !forceShow)
                return;
        }

        // Create and add
        StackFillButton button = new StackFillButton(
                screen,
                container,
                referenceSlot,
                isPlayerInv,
                policy,
                object.getClass().getName(),
                getShiftedOffset(offset, isPlayerInv)
        );
        addButton(screen, button, isPlayerInv);
    }

    private static void generateTransferButton(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean isPlayerInv,
            boolean forceShow
    ) {
        // Sanity check; we need a player to work with
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        // Sanity check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv)
            return;

        // Preliminary check; never display buttons on basic workstations or other minor inventories
        if (getNumberOfBulkInventorySlots(screen, isPlayerInv) < 3)
            return;

        // Get the relevant container, if any
        @Nullable Container container = isPlayerInv
                ? player.getInventory()
                : getContainer(player);
        // Sanity check; we need a container to work with
        if (container == null)
            return;

        // Select the relevant container or GUI class
        Object object = container instanceof SimpleContainer
                ? screen.getMenu()
                : container;

        // Check the relevant policy, if any
        @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());
        if ((policy == null || !policy.showTransferButton()) && !forceShow)
            return;

        // Get the configured or default offset
        Vec2i offset = policy != null
                ? policy.getButtonOffset()
                : options().layoutOffset;

        // Get the destination container, if any
        @Nullable Container dstContainer = isPlayerInv
                ? getContainer(player)
                : player.getInventory();
        if (dstContainer != null) {
            // Select the relevant container or GUI class
            Object dstObject = dstContainer instanceof SimpleContainer
                    ? screen.getMenu()
                    : dstContainer;

            // Check the relevant policy, if any
            @Nullable ClassPolicy dstPolicy = PolicyManager.getPolicy(dstObject.getClass());
            if ((dstPolicy == null || !dstPolicy.showTransferButton()) && !forceShow)
                return;
        }

        // Create and add
        TransferButton button = new TransferButton(
                screen,
                container,
                referenceSlot,
                isPlayerInv,
                policy,
                object.getClass().getName(),
                getShiftedOffset(offset, isPlayerInv)
        );
        addButton(screen, button, isPlayerInv);
    }

    /**
     * @return the container associated with the player's container menu, if it exists.
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
            TriggerButton button,
            boolean isPlayerInv
    ) {
        ((ScreenAccessor) screen).clientsort$addRenderableWidget(button);
        (isPlayerInv ? playerButtons : containerButtons).add(button);
    }

    /**
     * @return the slot to which a button position in the respective container should be anchored,
     * if any are available.
     */
    private static @Nullable Slot getReferenceSlot(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv
    ) {
        // Get the top-most of the right-most slots in scope
        ContainerScreenHelper<?> helper = ContainerScreenHelper.of(screen);
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInv
                        ? (slot.container instanceof Inventory)
                        && helper.getScope(slot).equals(Scope.PLAYER_INV)
                        : !(slot.container instanceof Inventory)
                                && helper.getScope(slot).equals(Scope.CONTAINER_INV))
                .max(Comparator.comparingInt(slot -> slot.x * 9999 - slot.y))
                .orElse(null);
    }

    /**
     * @return the number of slots in the respective container that are theoretically able to store
     * any item.
     */
    private static int getNumberOfBulkInventorySlots(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv
    ) {
        return screen.getMenu().slots.stream()
                .filter(slot -> isPlayerInv == (slot.container instanceof Inventory))
                .filter(slot -> !(screen.getMenu() instanceof HorseInventoryMenu)
                        || ((ISlot) slot).clientsort$getIndexInInv() >= 2)
                .mapToInt(slot -> 1)
                .sum();
    }

    /**
     * @return the offset, shifted by a constant amount based on the number of buttons already
     * generated.
     */
    @SuppressWarnings("ConstantValue")
    public static Vec2i getShiftedOffset(Vec2i offset, boolean isPlayerInv) {
        int index = (isPlayerInv ? playerButtons : containerButtons).size();

        int x = offset.x() + BUTTON_SHIFT_X * (TriggerButton.WIDTH + BUTTON_SPACING) * index;
        int y = offset.y() + BUTTON_SHIFT_Y * (TriggerButton.HEIGHT + BUTTON_SPACING) * index;

        return new Vec2i(x, y);
    }
}
