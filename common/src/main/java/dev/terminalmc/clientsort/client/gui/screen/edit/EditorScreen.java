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

package dev.terminalmc.clientsort.client.gui.screen.edit;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Policy;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.gui.widget.TriggerButton;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public abstract class EditorScreen extends Screen {

    private final Screen lastScreen;
    private final AbstractContainerScreen<?> underlay;
    private final LinkedList<TriggerButton> buttons = new LinkedList<>();
    public final Set<Integer> ignoredSlots = new TreeSet<>();

    /**
     * An element of {@link EditorScreen#buttons} which 'represents' the whole set of
     * buttons.
     * <p>
     * This can be any element, and the specific choice is only relevant when repositioning via
     * mouse drag.
     */
    private TriggerButton rep;

    /**
     * The class name of either {@link EditorScreen#rep}'s {@link TriggerButton#container}, or
     * {@link EditorScreen#underlay}'s {@link AbstractContainerScreen#getMenu} if the former
     * is {@code null}.
     * <p>
     * This value represents the lowest-level key on which a {@link ClassPolicy} can be created, and
     * may differ from {@link EditorScreen#rep}'s {@link TriggerButton#activePolicyKey}.
     */
    private String lowestPolicyKey;

    /**
     * A flag to assist repositioning buttons via click-drag.
     */
    private boolean dragging;

    public EditorScreen(AbstractContainerScreen<?> underlay, TriggerButton button) {
        this(underlay, button, underlay);
    }

    public EditorScreen(
            AbstractContainerScreen<?> underlay,
            TriggerButton button,
            Screen lastScreen
    ) {
        super(localized("title", "positionEditor"));
        this.font = Minecraft.getInstance().font;
        this.lastScreen = lastScreen;
        this.underlay = underlay;
        this.rep = button;
        this.buttons.add(button);
    }

    /**
     * Re-initializes {@link EditorScreen#underlay}, then this screen's GUI elements.
     */
    @Override
    public void init() {
        super.init();

        // Resize the underlay
        underlay.init(Minecraft.getInstance(), width, height);

        // Reload buttons from the manager
        if (!reloadButtonsAndIgnoredSlots()) {
            // Failure
            clearWidgets();
            return;
        }

        // Populate the GUI
        rebuildGui();
    }

    /**
     * Reloads the lists of editable buttons and ignored slots.
     */
    private boolean reloadButtonsAndIgnoredSlots() {
        buttons.clear();
        ignoredSlots.clear();

        // Retrieve the buttons from the manager
        buttons.addAll(getButtons());

        if (buttons.size() != 4) {
            if (debug()) {
                ClientSort.LOG.error(
                        "Failed to reload buttons on PositionEditScreen: Button list is too small (expected: {}, actual: {})",
                        4,
                        buttons.size()
                );
            }
            return false;
        }

        // Pick an arbitrary representative
        rep = buttons.getFirst();

        // Activate buttons that are enabled in config but inactive due to GUI state, such as
        // transfer buttons when no second inventory is open
        ClassPolicy policy = options().classPolicies.get(rep.activePolicyKey);
        if (policy != null) {
            buttons.forEach((button) -> button.active = button.getPolicyStatus(policy));
            ignoredSlots.addAll(policy.ignoredSlots());
        }

        // Retrieve the policy key in the same way as the manager
        Object keyObject =
                rep.container instanceof SimpleContainer ? underlay.getMenu() : rep.container;
        lowestPolicyKey = keyObject.getClass().getName();

        return true;
    }

    /**
     * Retrieves the list of editable buttons from the source.
     */
    protected abstract LinkedList<TriggerButton> getButtons();

    /**
     * Clears and re-populates this screen's GUI.
     */
    private void rebuildGui() {
        clearWidgets();

        StringWidget titleWidget = new StringWidget(0, 2, width, font.lineHeight, title, font);
        addRenderableWidget(titleWidget);

        int numButtons = 10;
        int x = 2;
        int movingY = height - 21 * numButtons;
        int width = 100;
        int height = 20;

        // Instructions tooltip button
        Button instructionsButton = Button.builder(
                        localized("editor", "instructions"), (button) -> {
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "instructions.tooltip.1")
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.2"))
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.3"))
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.4"))
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.5"))))
                .pos(x, movingY)
                .size(width, height)
                .build();
        instructionsButton.active = false;
        addRenderableWidget(instructionsButton);
        movingY += 21;

        // Toggle the layout status of all buttons
        Button toggleVisibilityButton = Button.builder(
                        localized("editor", "toggleVisibility"), (button) -> {
                            boolean status = buttons.stream().noneMatch((b) -> b.active);
                            buttons.forEach((b) -> b.active = status);
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "toggleVisibility.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(toggleVisibilityButton);
        movingY += 21;

        // Clear the ignored slots list
        Button unignoreSlotsButton = Button.builder(
                        localized("editor", "unignoreSlots"), (button) -> ignoredSlots.clear()
                )
                .tooltip(Tooltip.create(localized("editor", "unignoreSlots.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(unignoreSlotsButton);
        movingY += 21;

        // Move the button to the default position
        Button moveToDefaultButton = Button.builder(
                        localized("editor", "moveToDefault"), (button) -> {
                            Vec2i before = buttons.getFirst().offset;
                            buttons.getFirst().offset = options().layoutOffset;
                            repositionButtons(buttons.getFirst(), before);
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "moveToDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(moveToDefaultButton);
        movingY += 21;

        // Save the current position as default
        Button saveAsDefaultButton = Button.builder(
                        localized("editor", "saveAsDefault"),
                        (button) -> Minecraft.getInstance().setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        options().layoutOffset = buttons.getFirst().offset;
                                        Config.save();
                                        init();
                                    }
                                    Minecraft.getInstance().setScreen(this);
                                },
                                localized("title", "confirm.saveAsDefault"),
                                localized("message", "confirm.saveAsDefault")
                        ))
                )
                .tooltip(Tooltip.create(localized("editor", "saveAsDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(saveAsDefaultButton);
        movingY += 21;

        // Split the current config off the parent class key
        Button splitPolicyButton = Button.builder(
                        localized("editor", "splitPolicy"),
                        (button) -> Minecraft.getInstance().setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        options().classPolicies.put(
                                                lowestPolicyKey, new ClassPolicy(
                                                        lowestPolicyKey,
                                                        buttons.getFirst().offset,
                                                        buttons.getFirst().operationAllowed
                                                                ? buttons.getFirst().active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        buttons.get(1).operationAllowed
                                                                ? buttons.getFirst().active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        buttons.get(2).operationAllowed
                                                                ? buttons.getFirst().active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        buttons.get(3).operationAllowed
                                                                ? buttons.getFirst().active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        new TreeSet<>(ignoredSlots)
                                                )
                                        );
                                        Config.save();
                                        init();
                                    }
                                    Minecraft.getInstance().setScreen(this);
                                },
                                localized("title", "confirm.splitPolicy"),
                                localized(
                                        "message",
                                        "confirm.splitPolicy",
                                        Component.literal(rep.activePolicyKey == null
                                                ? lowestPolicyKey
                                                : rep.activePolicyKey
                                        ).withStyle(ChatFormatting.GOLD),
                                        Component.literal(lowestPolicyKey)
                                                .withStyle(ChatFormatting.GOLD)
                                )
                        ))
                )
                .tooltip(Tooltip.create(localized("editor", "splitPolicy.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        splitPolicyButton.active =
                rep.activePolicyKey != null && !rep.activePolicyKey.equals(lowestPolicyKey);
        addRenderableWidget(splitPolicyButton);
        movingY += 21;

        // Re-generates the screen to undo all changes made since opening
        Button undoChangesButton = Button.builder(
                        localized("editor", "undoChanges"),
                        (button) -> init()
                )
                .tooltip(Tooltip.create(localized("editor", "undoChanges.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(undoChangesButton);
        movingY += 21;

        // Open group selector screen
        Button reselectButton = Button.builder(
                        localized("editor", "reselect"),
                        (button) -> {
                            onClose();
                            Minecraft.getInstance().setScreen(
                                    new SelectorScreen(underlay, this)
                            );
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "reselect.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(reselectButton);
        movingY += 21;

        // Close this screen without saving
        Button cancelButton = Button.builder(CommonComponents.GUI_CANCEL, (button) -> onClose())
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(cancelButton);
        movingY += 21;

        // Save all changes then close this screen
        Button doneButton = Button.builder(CommonComponents.GUI_DONE, (button) -> saveAndClose())
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(doneButton);
    }

    /**
     * Renders the underlay, then this screen with its GUI.
     */
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        underlay.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        // Render disabled-slot indicators
        for (Slot slot : underlay.getMenu().slots) {
            Object object = getObj(slot, underlay.getMenu());
            if (object != null && object.getClass().getName().equals(lowestPolicyKey)) {
                if (ignoredSlots.contains(((ISlot) slot).clientsort$getIndexInContainer())) {
                    // Draw lock icon, top left
                    //noinspection UnnecessaryUnicodeEscape
                    graphics.drawString(
                            Minecraft.getInstance().font,
                            "\u274C",
                            ((AbstractContainerScreenAccessor) (underlay)).clientsort$getLeftPos()
                                    + slot.x,
                            ((AbstractContainerScreenAccessor) (underlay)).clientsort$getTopPos()
                                    + slot.y,
                            0xFF0000
                    );
                }
            }
        }

        // Safety net
        if (buttons.isEmpty())
            return;

        // Render trace lines
        drawLineFor(graphics, buttons.getFirst());

        // Render info lines
        Vec2i offset = buttons.getFirst().offset;
        graphics.drawString(
                font,
                localized("editor", "offset", offset.x(), offset.y()).getString(),
                105,
                height - (font.lineHeight + 1) * 3,
                0xFFFFFFFF
        );
        graphics.drawString(
                font,
                localized(
                        "editor",
                        "policyKey.current",
                        rep.activePolicyKey
                                == null
                                ? localized("editor", "policyKey.unset")
                                : rep.activePolicyKey
                ),
                105,
                height - (font.lineHeight + 1) * 2,
                0xFFFFFFFF
        );
        graphics.drawString(
                font,
                localized("editor", "policyKey.menu", lowestPolicyKey),
                105,
                height - (font.lineHeight + 1),
                0xFFFFFFFF
        );

        // Render editable widgets again, above background blur
        for (TriggerButton cb : buttons) {
            cb.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Modifies the background blur to be constant irrespective of the configured value.
     * <p>
     * Minimal blur is used to prevent the editable widgets disappearing under underlay items on a
     * higher render layer, while still keeping the underlay detail discernible.
     */
    @Override
    protected void renderBlurredBackground(float partialTick) {
        int original = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(1);
        super.renderBlurredBackground(partialTick);
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(original);
    }

    /**
     * Draws a horizontal and a vertical line to trace this widget back to its positional origin
     * point.
     */
    private void drawLineFor(GuiGraphics graphics, TriggerButton button) {
        graphics.hLine(button.getX() - button.offset.x(), button.getX(), button.getY(), 0xFFBBBBBB);
        graphics.vLine(
                button.getX() - button.offset.x(),
                button.getY() - button.offset.y(),
                button.getY(),
                0xFFBBBBBB
        );
    }

    /**
     * Closes this screen and shows {@link EditorScreen#lastScreen} instead.
     */
    @Override
    public void onClose() {
        super.onClose();
        lastScreen.init(Minecraft.getInstance(), width, height);
        Minecraft.getInstance().setScreen(lastScreen);
    }

    /**
     * Saves any altered values, then calls {@link EditorScreen#onClose}.
     */
    public void saveAndClose() {
        @Nullable Vec2i offset = buttons.getFirst().offset.equals(options().layoutOffset)
                ? null
                : buttons.getFirst().offset;
        buttons.forEach((b) -> b.savePolicy(offset, ignoredSlots));
        Config.save();
        onClose();
    }

    /**
     * Allows pressing the arrow keys to reposition the set of buttons.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int distance = Screen.hasShiftDown() ? 6 : 1;
        @Nullable Vec2i movement = switch (keyCode) {
            case InputConstants.KEY_LEFT -> new Vec2i(-distance, 0);
            case InputConstants.KEY_RIGHT -> new Vec2i(distance, 0);
            case InputConstants.KEY_UP -> new Vec2i(0, -distance);
            case InputConstants.KEY_DOWN -> new Vec2i(0, distance);
            default -> null;
        };
        if (movement != null) {
            Vec2i before = rep.offset;
            // Move the rep button first
            rep.offset = rep.offset.add(movement);
            // Then move the others to match
            repositionButtons(rep, before);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Allows dragging the selected widget to reposition it.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            dragging = false;
            return true;
        } else {
            for (TriggerButton cb : buttons) {
                if (cb.isMouseOver(mouseX, mouseY)) {
                    cb.mouseClicked(mouseX, mouseY, mouseButton);
                    rep = cb;
                    dragging = true;
                    return true;
                }
            }
            for (Slot slot : underlay.getMenu().slots) {
                if (((AbstractContainerScreenAccessor) underlay)
                        .clientsort$isHovering(slot, mouseX, mouseY)) {
                    Object object = getObj(slot, underlay.getMenu());
                    if (object != null && object.getClass().getName().equals(lowestPolicyKey)) {
                        int slotId = ((ISlot) slot).clientsort$getIndexInContainer();
                        if (ignoredSlots.contains(slotId))
                            ignoredSlots.remove(slotId);
                        else
                            ignoredSlots.add(slotId);
                    }
                }
            }
            return false;
        }
    }

    /**
     * Allows dragging the selected widget to reposition it.
     */
    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (dragging) {
            Vec2i before = rep.offset;
            if (rep.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                // Move the other buttons to match the rep's movement
                repositionButtons(rep, before);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Allows dragging the selected widget to reposition it.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    /**
     * If {@code before} does not match the current buttonOffset of {@code button}, moves all other
     * widgets to match how {@code button} was moved.
     */
    private void repositionButtons(TriggerButton button, Vec2i before) {
        if (!button.offset.equals(before)) {
            Vec2i diff = button.offset.subtract(before);
            for (TriggerButton cb : buttons) {
                if (cb != button) {
                    cb.offset = cb.offset.add(diff);
                }
            }
        }
    }
}
