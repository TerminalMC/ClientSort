/*
 * Copyright 2021 Evan Steinkerchner (Roundaround)
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

package dev.terminalmc.clientsort.client.gui;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Operation;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.gui.screen.edit.ContainerEditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.PlayerEditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.SelectorScreen;
import dev.terminalmc.clientsort.client.gui.widget.*;
import dev.terminalmc.clientsort.client.inventory.Scope;
import dev.terminalmc.clientsort.client.inventory.helper.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.mixin.client.accessor.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public class TriggerButtonManager {

    private TriggerButtonManager() {
    }

    private static final int BUTTON_SPACING = 1;

    private static @Nullable AbstractContainerScreen<?> screen;

    private static final LinkedList<TriggerButton> containerButtons = new LinkedList<>();
    private static final LinkedList<TriggerButton> visibleContainerButtons = new LinkedList<>();
    private static final LinkedList<TriggerButton> playerButtons = new LinkedList<>();
    private static final LinkedList<TriggerButton> visiblePlayerButtons = new LinkedList<>();

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

    private static @Nullable Slot getRefSlot(LinkedList<TriggerButton> buttons, Operation op) {
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
     * Generates zero or more trigger buttons in accordance with config and state, and if any were
     * generated, adds them to the screen.
     */
    public static void afterScreenInit(Screen initScreen) {
        if (!(initScreen instanceof AbstractContainerScreen<?> acs))
            return;
        if (Minecraft.getInstance().player.isSpectator())
            return;

        if (screen != null) {
            TriggerButtonManager.getContainerButtons()
                    .forEach((b) -> ((ScreenAccessor) screen).clientsort$removeWidget(b));
            TriggerButtonManager.getPlayerButtons()
                    .forEach((b) -> ((ScreenAccessor) screen).clientsort$removeWidget(b));
        }
        containerButtons.clear();
        visibleContainerButtons.clear();
        playerButtons.clear();
        visiblePlayerButtons.clear();
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

        boolean left = options().anchorButtonsLeft;
        boolean justifyLeft = options().justifyButtonsTopLeft;


        List<Operation> ops;
        if (justifyLeft) {
            ops = List.of(
                    options().firstButtonOp,
                    options().secondButtonOp,
                    options().thirdButtonOp,
                    options().fourthButtonOp
            );
        } else {
            ops = List.of(
                    options().fourthButtonOp,
                    options().thirdButtonOp,
                    options().secondButtonOp,
                    options().firstButtonOp
            );
        }

        // Generate container-side buttons
        Slot refSlotC = getReferenceSlot(acs, false, left);
        if (refSlotC != null) {
            boolean isEditor = isEditorC;
            ops.forEach((op) -> generate(
                    acs,
                    refSlotC,
                    left,
                    justifyLeft,
                    false,
                    isEditor,
                    enabled,
                    op
            ));
        }

        // Generate player-side buttons
        Slot refSlotP = getReferenceSlot(acs, true, left);
        if (refSlotP != null) {
            boolean isEditor = isEditorP;
            ops.forEach((op) -> generate(
                    acs,
                    refSlotP,
                    left,
                    justifyLeft,
                    true,
                    isEditor,
                    enabled,
                    op
            ));
        }
    }

    /**
     * Generates zero or one config buttons in accordance with params and config, and if a button
     * was generated, adds it to the screen.
     */
    private static void generate(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean referenceLeft,
            boolean justifyTopLeft,
            boolean isPlayerInv,
            boolean isEditor,
            boolean enabled,
            Operation op
    ) {
        switch (op) {
            case SORT -> generateSimpleButton(
                    screen,
                    referenceSlot,
                    referenceLeft,
                    justifyTopLeft,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canSort,
                    ClassPolicy::showSortButton,
                    ClassPolicy::autoSort,
                    SortButton::new,
                    localized("key", "op.sort")
            );
            case STACK_FILL -> generateDirectionalButton(
                    screen,
                    referenceSlot,
                    referenceLeft,
                    justifyTopLeft,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canStackFill,
                    ClassPolicy::showStackFillButton,
                    ClassPolicy::autoStackFill,
                    StackFillButton::new,
                    localized("key", "op.stackFill")
            );
            case MATCH_TRANSFER -> generateDirectionalButton(
                    screen,
                    referenceSlot,
                    referenceLeft,
                    justifyTopLeft,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canMatchTransfer,
                    ClassPolicy::showMatchTransferButton,
                    ClassPolicy::autoMatchTransfer,
                    MatchTransferButton::new,
                    localized("key", "op.matchTransfer")
            );
            case TRANSFER -> generateDirectionalButton(
                    screen,
                    referenceSlot,
                    referenceLeft,
                    justifyTopLeft,
                    isPlayerInv,
                    isEditor,
                    enabled,
                    ClassPolicy::canTransfer,
                    ClassPolicy::showTransferButton,
                    ClassPolicy::autoTransfer,
                    TransferButton::new,
                    localized("key", "op.transfer")
            );
        }
    }

    private static void generateSimpleButton(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean referenceLeft,
            boolean justifyTopLeft,
            boolean isPlayerInv,
            boolean isEditor,
            boolean enabled,
            Function<ClassPolicy, Boolean> opCheck,
            Function<ClassPolicy, Boolean> buttonCheck,
            Function<ClassPolicy, Boolean> autoCheck,
            TriggerButtonCreator creator,
            Component name
    ) {
        boolean autoPress = false;

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

        // Select the relevant title
        Component invTitle = isPlayerInv
                ? ((AbstractContainerScreenAccessor) screen).clientsort$getPlayerInventoryTitle()
                : screen.getTitle();

        // Retrieve the relevant policy, if any
        @Nullable ClassPolicy policy =
                PolicyManager.getPolicy(object.getClass(), invTitle.getString());

        // Evaluate display mode
        boolean create = isEditor || policy == null || opCheck.apply(policy);
        boolean add = enabled && (isEditor || (policy != null && buttonCheck.apply(policy)));
        if (!create)
            return;

        // Get the configured or default offset
        Vec2i offset = policy != null
                ? policy.getButtonOffset()
                : options().layoutOffset;

        // Check the auto trigger
        if (policy != null && autoCheck.apply(policy))
            autoPress = true;

        // Create and add
        TriggerButton button = creator.create(
                screen,
                container,
                referenceSlot,
                referenceLeft,
                isPlayerInv,
                policy,
                ClassPolicy.getKey(object.getClass().getName(), null),
                getShiftedOffset(offset, isPlayerInv, justifyTopLeft),
                name
        );
        BiConsumer<LinkedList<TriggerButton>, TriggerButton> adder = justifyTopLeft
                ? LinkedList::add
                : LinkedList::addFirst;
        adder.accept(isPlayerInv ? playerButtons : containerButtons, button);
        if (add) {
            adder.accept(isPlayerInv ? visiblePlayerButtons : visibleContainerButtons, button);
            ((ScreenAccessor) screen).clientsort$addRenderableWidget(button);
        }
        if (autoPress) {
            ClientSort.taskManager.schedule(
                    isPlayerInv ? options().autoOpDelayPlayer : options().autoOpDelayContainer,
                    () -> {
                        if (Minecraft.getInstance().screen == screen
                                && !KeybindManager.isDown(KeybindManager.CANCEL_AUTO_KEY)
                                && (isPlayerInv ? playerButtons : containerButtons)
                                .contains(button)) {
                            button.onPress();
                        }
                    }
            );
        }
    }

    private static void generateDirectionalButton(
            AbstractContainerScreen<?> screen,
            Slot referenceSlot,
            boolean referenceLeft,
            boolean justifyTopLeft,
            boolean isPlayerInv,
            boolean isEditor,
            boolean enabled,
            Function<ClassPolicy, Boolean> opCheck,
            Function<ClassPolicy, Boolean> buttonCheck,
            Function<ClassPolicy, Boolean> autoCheck,
            TriggerButtonCreator creator,
            Component name
    ) {
        boolean autoPress = false;

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

        // Select the relevant title
        Component invTitle = isPlayerInv
                ? ((AbstractContainerScreenAccessor) screen).clientsort$getPlayerInventoryTitle()
                : screen.getTitle();

        // Retrieve the relevant policy, if any
        @Nullable ClassPolicy policy =
                PolicyManager.getPolicy(object.getClass(), invTitle.getString());

        // Evaluate display mode
        boolean create = isEditor || policy == null || opCheck.apply(policy);
        boolean add = enabled && (isEditor || (policy != null && buttonCheck.apply(policy)));
        if (!create)
            return;

        // Get the configured or default offset
        Vec2i offset = policy != null
                ? policy.getButtonOffset()
                : options().layoutOffset;

        // Check the auto trigger
        if (policy != null && autoCheck.apply(policy) && !policy.autoOpOther())
            autoPress = true;

        // Get the destination container, if any
        @Nullable Container dstContainer = isPlayerInv
                ? getContainer(player)
                : player.getInventory();
        if (dstContainer != null) {
            // Select the relevant container or GUI class
            Object dstObject = getObj(dstContainer, screen.getMenu());
            if (dstObject == null)
                return;

            // Select the relevant title
            Component dstInvTitle = isPlayerInv
                    ? screen.getTitle()
                    : ((AbstractContainerScreenAccessor) screen).clientsort$getPlayerInventoryTitle();

            // Retrieve the relevant policy, if any
            @Nullable ClassPolicy dstPolicy =
                    PolicyManager.getPolicy(dstObject.getClass(), dstInvTitle.getString());

            // Re-evaluate display mode
            create = isEditor || dstPolicy == null || opCheck.apply(dstPolicy);
            add = add && (isEditor || (dstPolicy != null && buttonCheck.apply(dstPolicy)));
            if (!create)
                return;

            // Check the auto trigger
            if (dstPolicy != null && autoCheck.apply(dstPolicy) && dstPolicy.autoOpOther())
                autoPress = true;
        }

        // Create and add
        TriggerButton button = creator.create(
                screen,
                container,
                referenceSlot,
                referenceLeft,
                isPlayerInv,
                policy,
                ClassPolicy.getKey(object.getClass().getName(), null),
                getShiftedOffset(offset, isPlayerInv, justifyTopLeft),
                name
        );
        BiConsumer<LinkedList<TriggerButton>, TriggerButton> adder = justifyTopLeft
                ? LinkedList::add
                : LinkedList::addFirst;
        adder.accept(isPlayerInv ? playerButtons : containerButtons, button);
        if (add) {
            adder.accept(isPlayerInv ? visiblePlayerButtons : visibleContainerButtons, button);
            ((ScreenAccessor) screen).clientsort$addRenderableWidget(button);
        }
        if (autoPress) {
            ClientSort.taskManager.schedule(
                    isPlayerInv ? options().autoOpDelayPlayer : options().autoOpDelayContainer,
                    () -> {
                        if (Minecraft.getInstance().screen == screen
                                && !KeybindManager.isDown(KeybindManager.CANCEL_AUTO_KEY)
                                && (isPlayerInv ? playerButtons : containerButtons)
                                .contains(button)) {
                            button.onPress();
                        }
                    }
            );
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
            boolean isPlayerInv,
            boolean anchorButtonsLeft
    ) {
        ContainerScreenHelper<?> helper = ContainerScreenHelper.of(screen);
        Slot bestSlot = null;
        double bestScore = 0;

        Scope scope = isPlayerInv ? Scope.PLAYER_INV : Scope.CONTAINER_INV;
        for (Slot slot : helper.getLargestSlotGroup(scope)) {
            // Calculate the weighted positional score

            // x factor is how far from the left side of the menu the slot is, as a fraction
            // of the screen width
            double xFactor = Mth.clamp(slot.x, 0, screen.width) / (double) screen.width;
            // y factor is how far from the top of the menu the slot is, as a fraction of the
            // screen height
            double yFactor = Mth.clamp(slot.y, 0, screen.height) / (double) screen.height;

            double x;
            double y;
            if (anchorButtonsLeft) {
                // Prefer further left
                x = 1 - xFactor;
            } else {
                // Prefer further right
                x = xFactor;
            }
            // Prefer higher up
            y = 1 - yFactor;

            // Assign weights
            double score = x * 0.8d + y * 0.2d;

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
    public static Vec2i getShiftedOffset(
            Vec2i offset,
            boolean isPlayerInv,
            boolean justifyTopLeft
    ) {
        int index = (isPlayerInv ? visiblePlayerButtons : visibleContainerButtons).size();
        boolean horizontal = options().buttonsHorizontal;

        int shiftX = horizontal ? 1 : 0;
        int shiftY = horizontal ? 0 : 1;

        int x, y;
        if (justifyTopLeft) {
            x = offset.x() + shiftX * (TriggerButton.WIDTH + BUTTON_SPACING) * index;
            y = offset.y() + shiftY * (TriggerButton.HEIGHT + BUTTON_SPACING) * index;
        } else {
            x = offset.x() - shiftX * (TriggerButton.WIDTH + BUTTON_SPACING) * index;
            y = offset.y() - shiftY * (TriggerButton.HEIGHT + BUTTON_SPACING) * index;
        }

        return new Vec2i(x, y);
    }

    public static LinkedList<TriggerButton> reversed(LinkedList<TriggerButton> list) {
        LinkedList<TriggerButton> newList = new LinkedList<>();
        list.forEach(newList::addFirst);
        return newList;
    }

    @FunctionalInterface
    public interface TriggerButtonCreator {

        TriggerButton create(
                AbstractContainerScreen<?> screen,
                Container container,
                Slot referenceSlot,
                boolean referenceLeft,
                boolean isPlayerInv,
                @Nullable ClassPolicy policy,
                String lowestPolicyKey,
                Vec2i offset,
                Component name
        );
    }
}
