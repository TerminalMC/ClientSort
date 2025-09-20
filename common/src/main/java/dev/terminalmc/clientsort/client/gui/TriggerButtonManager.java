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
import dev.terminalmc.clientsort.client.gui.widget.*;
import dev.terminalmc.clientsort.client.inventory.Scope;
import dev.terminalmc.clientsort.client.inventory.helper.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import dev.terminalmc.clientsort.mixin.client.accessor.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public class TriggerButtonManager {

    private TriggerButtonManager() {
    }

    private static final int BUTTON_SPACING = 1;
    private static final int BUTTON_SHIFT_X = 0;
    private static final int BUTTON_SHIFT_Y = 1;

    private static @Nullable AbstractContainerScreen<?> screen;

    private static final LinkedHashSet<TriggerButton> containerButtons = new LinkedHashSet<>();
    private static final LinkedHashSet<TriggerButton> visibleContainerButtons =
            new LinkedHashSet<>();
    private static final LinkedHashSet<TriggerButton> playerButtons = new LinkedHashSet<>();
    private static final LinkedHashSet<TriggerButton> visiblePlayerButtons = new LinkedHashSet<>();

    public static @Nullable AbstractContainerScreen<?> getScreen() {
        return screen;
    }

    public static LinkedList<TriggerButton> getContainerButtons() {
        return new LinkedList<>(containerButtons);
    }

    public static LinkedList<TriggerButton> getPlayerButtons() {
        return new LinkedList<>(playerButtons);
    }

    public static @Nullable Slot getContainerRefSlot(Operation op) {
        return getRefSlot(containerButtons, op);
    }

    public static @Nullable Slot getPlayerRefSlot(Operation op) {
        return getRefSlot(playerButtons, op);
    }

    private static @Nullable Slot getRefSlot(LinkedHashSet<TriggerButton> buttons, Operation op) {
        Class<? extends TriggerButton> clazz = switch (op) {
            case SORT -> SortButton.class;
            case STACK_FILL -> StackFillButton.class;
            case MATCH_TRANSFER -> MatchTransferButton.class;
            case TRANSFER -> TransferButton.class;
        };

        for (TriggerButton button : buttons) {
            if (button.getClass().equals(clazz)) {
                return button.referenceSlot;
            }
        }
        return null;
    }

    /**
     * Generates zero or more control buttons in accordance with config and state, and if any were
     * generated, adds them to the screen.
     */
    public static void afterScreenInit(Screen initScreen) {
        if (!(initScreen instanceof AbstractContainerScreen<?> acs))
            return;

        if (screen != null) {
            TriggerButtonManager.getContainerButtons()
                    .forEach((b) -> ((ScreenAccessor) screen).clientsort$removeWidget(b));
            TriggerButtonManager.getPlayerButtons()
                    .forEach((b) -> ((ScreenAccessor) screen).clientsort$removeWidget(b));
        }
        containerButtons.clear();
        playerButtons.clear();
        screen = acs;

        boolean enabled = options().showButtons;

        boolean isEditorC = false;
        boolean isEditorP = false;
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof SelectorScreen) {
            isEditorC = true;
            isEditorP = true;
        } else if (currentScreen instanceof ContainerEditorScreen) {
            isEditorC = true;
        } else if (currentScreen instanceof PlayerEditorScreen) {
            isEditorP = true;
        }

        // Generate container-side buttons
        Slot refSlotC = getReferenceSlot(acs, false);
        if (refSlotC != null) {
            generate(acs, refSlotC, false, isEditorC, enabled, options().firstButtonOp);
            generate(acs, refSlotC, false, isEditorC, enabled, options().secondButtonOp);
            generate(acs, refSlotC, false, isEditorC, enabled, options().thirdButtonOp);
            generate(acs, refSlotC, false, isEditorC, enabled, options().fourthButtonOp);
        }

        // Generate player-side buttons
        Slot refSlotP = getReferenceSlot(acs, true);
        if (refSlotP != null) {
            generate(acs, refSlotP, true, isEditorP, enabled, options().firstButtonOp);
            generate(acs, refSlotP, true, isEditorP, enabled, options().secondButtonOp);
            generate(acs, refSlotP, true, isEditorP, enabled, options().thirdButtonOp);
            generate(acs, refSlotP, true, isEditorP, enabled, options().fourthButtonOp);
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
            boolean isEditor,
            boolean enabled,
            Operation op
    ) {
        switch (op) {
            case SORT -> generateSimpleButton(
                    screen,
                    refSlot,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canSort,
                    ClassPolicy::showSortButton,
                    SortButton::new,
                    localized("key", "op.sort")
            );
            case STACK_FILL -> generateDirectionalButton(
                    screen,
                    refSlot,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canStackFill,
                    ClassPolicy::showStackFillButton,
                    StackFillButton::new,
                    localized("key", "op.stackFill")
            );
            case MATCH_TRANSFER -> generateDirectionalButton(
                    screen,
                    refSlot,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canMatchTransfer,
                    ClassPolicy::showMatchTransferButton,
                    MatchTransferButton::new,
                    localized("key", "op.matchTransfer")
            );
            case TRANSFER -> generateDirectionalButton(
                    screen,
                    refSlot,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canTransfer,
                    ClassPolicy::showTransferButton,
                    TransferButton::new,
                    localized("key", "op.transfer")
            );
        }
    }

    private static void generateSimpleButton(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean isPlayerInv,
            boolean isEditor,
            boolean enabled,
            Function<ClassPolicy, Boolean> opCheck,
            Function<ClassPolicy, Boolean> buttonCheck,
            TriggerButtonCreator creator,
            Component name
    ) {
        // Sanity check; we need a player to work with
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        // Sanity check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv)
            return;

        // Get the relevant container, if any
        @Nullable Container container = isPlayerInv
                ? player.getInventory()
                : getContainer(player);

        // Select the relevant container or GUI class
        Object object = getObj(container, screen.getMenu());
        if (object == null)
            return;

        // Retrieve the relevant policy, if any
        @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());

        // Evaluate display mode
        boolean create = isEditor || policy == null || opCheck.apply(policy);
        boolean add = enabled && (isEditor || (policy != null && buttonCheck.apply(policy)));
        if (!create)
            return;

        // Get the configured or default offset
        Vec2i offset = policy != null
                ? policy.getButtonOffset()
                : options().layoutOffset;

        // Create and add
        TriggerButton button = creator.create(
                screen,
                container,
                referenceSlot,
                isPlayerInv,
                policy,
                object.getClass().getName(),
                getShiftedOffset(offset, isPlayerInv),
                name
        );
        (isPlayerInv ? playerButtons : containerButtons).add(button);
        if (add) {
            (isPlayerInv ? visiblePlayerButtons : visibleContainerButtons).add(button);
            ((ScreenAccessor) screen).clientsort$addRenderableWidget(button);
        }
    }

    private static void generateDirectionalButton(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean isPlayerInv,
            boolean isEditor,
            boolean enabled,
            Function<ClassPolicy, Boolean> opCheck,
            Function<ClassPolicy, Boolean> buttonCheck,
            TriggerButtonCreator creator,
            Component name
    ) {
        // Sanity check; we need a player to work with
        @Nullable LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        // Sanity check; never display container buttons on player screen
        if (screen instanceof InventoryScreen && !isPlayerInv)
            return;

        // Get the relevant container, if any
        @Nullable Container container = isPlayerInv
                ? player.getInventory()
                : getContainer(player);

        // Select the relevant container or GUI class
        Object object = getObj(container, screen.getMenu());
        if (object == null)
            return;

        // Check the relevant policy, if any
        @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());

        // Evaluate display mode
        boolean create = isEditor || policy == null || opCheck.apply(policy);
        boolean add = enabled && (isEditor || (policy != null && buttonCheck.apply(policy)));
        if (!create)
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
            Object dstObject = getObj(dstContainer, screen.getMenu());
            if (dstObject == null)
                return;

            // Check the relevant policy, if any
            @Nullable ClassPolicy dstPolicy = PolicyManager.getPolicy(dstObject.getClass());

            // Re-evaluate display mode
            create = isEditor || dstPolicy == null || opCheck.apply(dstPolicy);
            add = add && (isEditor || (dstPolicy != null && buttonCheck.apply(dstPolicy)));
            if (!create)
                return;
        }

        // Create and add
        TriggerButton button = creator.create(
                screen,
                container,
                referenceSlot,
                isPlayerInv,
                policy,
                object.getClass().getName(),
                getShiftedOffset(offset, isPlayerInv),
                name
        );
        (isPlayerInv ? playerButtons : containerButtons).add(button);
        if (add) {
            (isPlayerInv ? visiblePlayerButtons : visibleContainerButtons).add(button);
            ((ScreenAccessor) screen).clientsort$addRenderableWidget(button);
        }
    }

    /**
     * @return the container associated with the player's container menu, if it exists.
     */
    public static @Nullable Container getContainer(Player player) {
        // Local class for tracking container scores
        class ScoredContainer {

            public final Container container;
            public int score;

            public ScoredContainer(Container container, int score) {
                this.container = container;
                this.score = score;
            }
        }

        Map<Container, ScoredContainer> map = new HashMap<>();
        for (Slot slot : player.containerMenu.slots) {
            // Ignore irrelevant slots
            //noinspection ConstantValue
            if (slot.container == null)
                continue;
            // Break on reaching inventory
            if (slot.container == player.getInventory() || slot.container instanceof Inventory)
                break;
            @Nullable ScoredContainer scoredContainer = map.get(slot.container);
            if (scoredContainer == null) {
                // First slot from this container, store it
                map.put(slot.container, new ScoredContainer(slot.container, 1));
            } else {
                scoredContainer.score++;
            }
        }
        if (map.isEmpty())
            return null;

        return map.values().stream().max(Comparator.comparingInt(s -> s.score)).get().container;
    }

    /**
     * @return the slot to which a button position in the respective container should be anchored,
     * if any are available.
     */
    private static @Nullable Slot getReferenceSlot(
            AbstractContainerScreen<?> screen,
            boolean isPlayerInv
    ) {
        ContainerScreenHelper<?> helper = ContainerScreenHelper.of(screen);
        Slot bestSlot = null;
        double bestScore = 0;

        Scope scope = isPlayerInv ? Scope.PLAYER_INV : Scope.CONTAINER_INV;
        for (Slot slot : helper.getLargestSlotGroup(scope)) {
            // Calculate the weighted positional score
            double x = Math.clamp(slot.x, 0, screen.width)
                    / (double) screen.width;
            double y = (screen.height - Math.clamp(slot.y, 0, screen.height))
                    / (double) screen.height;
            double score = x * 0.8D + y * 0.2D;

            if (score > bestScore) {
                bestSlot = slot;
                bestScore = score;
            }
        }

        return bestSlot;
    }

    /**
     * @return the offset, shifted by a constant amount based on the number of buttons already
     * generated.
     */
    @SuppressWarnings("ConstantValue")
    public static Vec2i getShiftedOffset(Vec2i offset, boolean isPlayerInv) {
        int index = (isPlayerInv ? visiblePlayerButtons : visibleContainerButtons).size();

        int x = offset.x() + BUTTON_SHIFT_X * (TriggerButton.WIDTH + BUTTON_SPACING) * index;
        int y = offset.y() + BUTTON_SHIFT_Y * (TriggerButton.HEIGHT + BUTTON_SPACING) * index;

        return new Vec2i(x, y);
    }

    @FunctionalInterface
    public interface TriggerButtonCreator {

        TriggerButton create(
                AbstractContainerScreen<?> screen,
                Container container,
                Slot referenceSlot,
                boolean isPlayerInv,
                @Nullable ClassPolicy policy,
                String lowestPolicyKey,
                Vec2i offset,
                Component name
        );
    }
}
