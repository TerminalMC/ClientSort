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
import dev.terminalmc.clientsort.client.config.Config.Options.ControlButtonType;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.gui.screen.edit.ContainerPositionEditScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.GroupSelectorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.PlayerPositionEditScreen;
import dev.terminalmc.clientsort.client.gui.widget.ControlButton;
import dev.terminalmc.clientsort.client.gui.widget.SortButton;
import dev.terminalmc.clientsort.client.gui.widget.StackFillButton;
import dev.terminalmc.clientsort.client.gui.widget.TransferButton;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.util.Scope;
import dev.terminalmc.clientsort.config.ClassPolicy;
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
import java.util.Set;
import java.util.stream.Collectors;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.client.config.Config.options;

public class ControlButtonManager {

    private ControlButtonManager() {
    }

    private static final int BUTTON_SPACING = 1;

    // Alters whether buttons are arrayed horizontally, vertically or :/
    private static final int BUTTON_SHIFT_X = 0;
    private static final int BUTTON_SHIFT_Y = 1;

    private static final Set<Class<?>> layoutClasses = new LinkedHashSet<>();
    private static final Set<Class<?>> policyClasses = new LinkedHashSet<>();

    private static final LinkedHashSet<ControlButton> containerButtons = new LinkedHashSet<>();
    private static final LinkedHashSet<ControlButton> playerButtons = new LinkedHashSet<>();

    public static LinkedList<ControlButton> getContainerButtons() {
        return new LinkedList<>(containerButtons);
    }

    public static LinkedList<ControlButton> getPlayerButtons() {
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
        if (currentScreen instanceof GroupSelectorScreen) {
            forceShowContainer = true;
            forceShowPlayer = true;
        } else if (currentScreen instanceof ContainerPositionEditScreen) {
            forceShowContainer = true;
        } else if (currentScreen instanceof PlayerPositionEditScreen) {
            forceShowPlayer = true;
        }

        // Generate container-side buttons
        Slot containerRefSlot = getReferenceSlot(acs, false);
        if (containerRefSlot != null) {
            generate(acs, containerRefSlot, false, forceShowContainer, options().firstButton);
            generate(acs, containerRefSlot, false, forceShowContainer, options().secondButton);
            generate(acs, containerRefSlot, false, forceShowContainer, options().thirdButton);
        }

        // Generate player-side buttons
        Slot playerRefSlot = getReferenceSlot(acs, true);
        if (playerRefSlot != null) {
            generate(acs, playerRefSlot, true, forceShowPlayer, options().firstButton);
            generate(acs, playerRefSlot, true, forceShowPlayer, options().secondButton);
            generate(acs, playerRefSlot, true, forceShowPlayer, options().thirdButton);
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
            ControlButtonType type
    ) {
        switch (type) {
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

        // Preliminary check; respect policy unless forced
        @Nullable ClassPolicy policy = getPolicy(object.getClass());
        boolean disabledByPolicy = policy != null && !policy.sortEnabled;
        if (disabledByPolicy && !forceShow)
            return;

        // Retrieve the associated layout, if any
        @Nullable ButtonLayout layout = getLayout(object.getClass());

        // Get the configured or default offset
        Vec2i offset = layout != null
                ? layout.getOffset()
                : options().layoutOffset;

        // Get the configured or default status
        boolean enabled = layout != null
                ? layout.isSortEnabled()
                : options().sortEnabled;

        // Only add the button if it's whitelisted or forced, and if it is forced, mark it inactive
        boolean active;
        if (layout != null && enabled) {
            active = true;
        } else if (forceShow) {
            active = false;
        } else {
            return;
        }

        // Get the layout key, if any
        @Nullable String layoutKey = layout != null
                ? layout.className()
                : null;
        // Get the 'lowest' potential layout key
        String lowestLayoutKey = object.getClass().getName();
        // Get the policy key, if any, else get the 'lowest' potential policy key
        String policyKey = policy != null
                ? policy.className
                : lowestLayoutKey;

        // Create and add
        SortButton button = new SortButton(
                screen,
                container,
                layoutKey,
                lowestLayoutKey,
                policyKey,
                disabledByPolicy,
                isPlayerInv,
                referenceSlot,
                getShiftedOffset(offset, isPlayerInv),
                active
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

        // Preliminary check; respect policy unless forced
        @Nullable ClassPolicy policy = getPolicy(object.getClass());
        boolean disabledByPolicy = policy != null && !policy.stackFillEnabled;
        if (disabledByPolicy && !forceShow)
            return;

        // Get the associated layout, if any
        @Nullable ButtonLayout layout = getLayout(object.getClass());

        // Get the configured or default offset
        Vec2i offset = layout != null
                ? layout.getOffset()
                : options().layoutOffset;

        // Get the configured or default status
        boolean enabled = layout != null
                ? layout.isStackFillEnabled()
                : options().stackFillEnabled;

        // Get the destination container, if any
        @Nullable Container dstContainer = isPlayerInv
                ? getContainer(player)
                : player.getInventory();

        // Get the destination layout
        @Nullable ButtonLayout dstLayout = null;
        if (dstContainer != null) {
            // Select the relevant container or GUI class
            Object dstObject = dstContainer instanceof SimpleContainer
                    ? screen.getMenu()
                    : dstContainer;

            // Preliminary check; respect policy unless forced
            @Nullable ClassPolicy dstPolicy = getPolicy(dstObject.getClass());
            if (dstPolicy != null && !dstPolicy.stackFillEnabled && !forceShow)
                return;

            // Get the associated layout, if any
            dstLayout = getLayout(dstObject.getClass());
        }

        // Get the configured or default status
        boolean dstEnabled = dstLayout != null
                ? dstLayout.isStackFillEnabled()
                : options().stackFillEnabled;

        // Only add the button if both it and the other are whitelisted, or it's forced, and if it
        // is forced, mark it inactive
        boolean active;
        if ((layout != null && enabled) && (dstLayout != null && dstEnabled)) {
            active = true;
        } else if (forceShow) {
            active = false;
        } else {
            return;
        }

        // Get the layout key, if any
        @Nullable String layoutKey = layout != null
                ? layout.className()
                : null;
        // Get the 'lowest' potential layout key
        String lowestLayoutKey = object.getClass().getName();
        // Get the policy key, if any, else get the 'lowest' potential policy key
        String policyKey = policy != null
                ? policy.className
                : lowestLayoutKey;

        // Create and add
        StackFillButton button = new StackFillButton(
                screen,
                container,
                layoutKey,
                lowestLayoutKey,
                policyKey,
                disabledByPolicy,
                isPlayerInv,
                referenceSlot,
                getShiftedOffset(offset, isPlayerInv),
                active
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

        // Preliminary check; respect policy unless forced
        @Nullable ClassPolicy policy = getPolicy(object.getClass());
        boolean disabledByPolicy = policy != null && !policy.transferEnabled;
        if (disabledByPolicy && !forceShow)
            return;

        // Get the associated layout, if any
        @Nullable ButtonLayout layout = getLayout(object.getClass());

        // Get the configured or default offset
        Vec2i offset = layout != null
                ? layout.getOffset()
                : options().layoutOffset;

        // Get the configured or default status
        boolean enabled = layout != null
                ? layout.isTransferEnabled()
                : options().transferEnabled;

        // Get the destination container, if any
        @Nullable Container dstContainer = isPlayerInv
                ? getContainer(player)
                : player.getInventory();

        // Get the destination layout
        @Nullable ButtonLayout dstLayout = null;
        if (dstContainer != null) {
            // Select the relevant container or GUI class
            Object dstObject = dstContainer instanceof SimpleContainer
                    ? screen.getMenu()
                    : dstContainer;

            // Preliminary check; respect policy unless forced
            @Nullable ClassPolicy dstPolicy = getPolicy(dstObject.getClass());
            if (dstPolicy != null && !dstPolicy.transferEnabled && !forceShow)
                return;

            // Get the associated layout, if any
            dstLayout = getLayout(dstObject.getClass());
        }

        // Get the configured or default status
        boolean dstEnabled = dstLayout != null
                ? dstLayout.isTransferEnabled()
                : options().transferEnabled;

        // Only add the button if both it and the other are whitelisted, or it's forced, and if it
        // is forced, mark it inactive
        boolean active;
        if ((layout != null && enabled) && (dstLayout != null && dstEnabled)) {
            active = true;
        } else if (forceShow) {
            active = false;
        } else {
            return;
        }

        // Get the layout key, if any
        @Nullable String layoutKey = layout != null
                ? layout.className()
                : null;
        // Get the 'lowest' potential layout key
        String lowestLayoutKey = object.getClass().getName();
        // Get the policy key, if any, else get the 'lowest' potential policy key
        String policyKey = policy != null
                ? policy.className
                : lowestLayoutKey;

        // Create and add
        TransferButton button = new TransferButton(
                screen,
                container,
                layoutKey,
                lowestLayoutKey,
                policyKey,
                disabledByPolicy,
                isPlayerInv,
                referenceSlot,
                getShiftedOffset(offset, isPlayerInv),
                active
        );
        addButton(screen, button, isPlayerInv);
    }

    /**
     * Reloads the cache of layout configuration classes.
     */
    public static void reloadLayoutClasses(Set<String> classNames) {
        layoutClasses.clear();
        for (String className : classNames) {
            try {
                layoutClasses.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                if (debug()) {
                    ClientSort.LOG.warn(
                            "Unable to load layout class '{}': Class not found.",
                            className
                    );
                }
            }
        }
    }

    /**
     * Reloads the cache of policy configuration classes.
     */
    public static void reloadPolicyClasses(Set<String> classNames) {
        policyClasses.clear();
        for (String className : classNames) {
            try {
                policyClasses.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                if (debug()) {
                    ClientSort.LOG.warn(
                            "Unable to load policy class '{}': Class not found.",
                            className
                    );
                }
            }
        }
    }

    /**
     * @return the lowest-degree matching layout for the specified class, if any exists.
     */
    public static @Nullable ButtonLayout getLayout(Class<?> cls) {
        // Check for a perfect match
        ButtonLayout layout = options().buttonLayouts.get(cls.getName());
        if (layout != null)
            return layout;

        // No perfect match; find all higher-degree matching classes
        Set<Class<?>> matches = layoutClasses.stream()
                .filter(c -> c.isAssignableFrom(cls))
                .collect(Collectors.toSet());

        // Double-iterate to find the lowest-degree match
        for (Class<?> c1 : matches) {
            boolean hasSubclass = false;
            // If any c2 is a subclass of c1, c1 is not lowest
            for (Class<?> c2 : matches) {
                if (!c1.equals(c2) && c1.isAssignableFrom(c2)) {
                    hasSubclass = true;
                    break;
                }
            }
            if (!hasSubclass) {
                // No subclass found; return layout for c1
                return options().buttonLayouts.get(c1.getName());
            }
        }
        return null;
    }

    /**
     * @return the lowest-degree matching policy for the specified class, if policies are enabled
     * and any exists.
     */
    public static @Nullable ClassPolicy getPolicy(Class<?> cls) {
        if (!options().applyPolicies)
            return null;

        // Check for a perfect match
        ClassPolicy policy = options().classPolicies.get(cls.getName());
        if (policy != null)
            return policy;

        // No perfect match; find all higher-degree matching classes
        Set<Class<?>> matches = policyClasses.stream()
                .filter(c -> c.isAssignableFrom(cls))
                .collect(Collectors.toSet());

        // Double-iterate to find the lowest-degree match
        for (Class<?> c1 : matches) {
            boolean hasSubclass = false;
            // If any c2 is a subclass of c1, c1 is not lowest
            for (Class<?> c2 : matches) {
                if (!c1.equals(c2) && c1.isAssignableFrom(c2)) {
                    hasSubclass = true;
                    break;
                }
            }
            if (!hasSubclass) {
                // No subclass found; return policy for c1
                return options().classPolicies.get(c1.getName());
            }
        }
        return null;
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
            ControlButton button,
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

        int x = offset.x() + BUTTON_SHIFT_X * (ControlButton.WIDTH + BUTTON_SPACING) * index;
        int y = offset.y() + BUTTON_SHIFT_Y * (ControlButton.HEIGHT + BUTTON_SPACING) * index;

        return new Vec2i(x, y);
    }
}
