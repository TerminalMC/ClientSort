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
import dev.terminalmc.clientsort.client.config.ButtonLayout;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.gui.widget.ControlButton;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public abstract class PositionEditScreen extends Screen {

    private final Screen lastScreen;
    private final AbstractContainerScreen<?> underlay;
    private final LinkedList<ControlButton> buttons = new LinkedList<>();

    /**
     * An element of {@link PositionEditScreen#buttons} which 'represents' the whole set of
     * buttons.
     * <p>
     * This can be any element, and the specific choice is only relevant when repositioning via
     * mouse drag.
     */
    private ControlButton rep;

    /**
     * The class name of either {@link PositionEditScreen#rep}'s {@link ControlButton#container}, or
     * {@link PositionEditScreen#underlay}'s {@link AbstractContainerScreen#getMenu} if the former
     * is {@code null}.
     * <p>
     * This value represents the lowest-level key on which a {@link ButtonLayout} can be created,
     * and may differ from {@link PositionEditScreen#rep}'s {@link ControlButton#layoutKey}.
     */
    private String lowestLayoutKey;

    /**
     * A flag to assist repositioning buttons via click-drag.
     */
    private boolean dragging;

    public PositionEditScreen(AbstractContainerScreen<?> underlay, ControlButton button) {
        this(underlay, button, underlay);
    }

    public PositionEditScreen(
            AbstractContainerScreen<?> underlay,
            ControlButton button,
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
     * Re-initializes {@link PositionEditScreen#underlay}, then this screen's GUI elements.
     */
    @Override
    public void init() {
        super.init();

        // Resize the underlay
        underlay.init(Minecraft.getInstance(), width, height);

        // Reload buttons from the manager
        if (!reloadButtons()) {
            // Failure
            clearWidgets();
            return;
        }

        // Populate the GUI
        rebuildGui();
    }

    /**
     * Reloads the list of editable buttons.
     */
    private boolean reloadButtons() {
        buttons.clear();

        // Retrieve the buttons from the manager
        buttons.addAll(getButtons());

        if (buttons.size() != 3) {
            if (debug()) {
                ClientSort.LOG.error(
                        "Failed to reload buttons on PositionEditScreen: Button list is too small (expected: {}, actual: {})",
                        3,
                        buttons.size()
                );
            }
            return false;
        }

        // Pick an arbitrary representative
        rep = buttons.getFirst();

        // Enable buttons that are enabled in config but disabled due to GUI
        // state, such as transfer buttons when no second inventory is open
        ButtonLayout layout = options().buttonLayouts.get(rep.layoutKey);
        if (layout != null) {
            buttons.forEach((button) -> button.active = button.getLayoutStatus(layout));
        }

        // Retrieve the layout key in the same way as the manager
        Object keyObject =
                rep.container instanceof SimpleContainer ? underlay.getMenu() : rep.container;
        lowestLayoutKey = keyObject.getClass().getName();

        return true;
    }

    /**
     * Retrieves the list of editable buttons from the source.
     */
    protected abstract LinkedList<ControlButton> getButtons();

    /**
     * Clears and re-populates this screen's GUI.
     */
    private void rebuildGui() {
        clearWidgets();

        StringWidget titleWidget = new StringWidget(0, 2, width, font.lineHeight, title, font);
        addRenderableWidget(titleWidget);

        int numButtons = 9;
        int x = 2;
        int movingY = height - 21 * numButtons;
        int width = 100;
        int height = 20;

        // Instructions tooltip button
        Button instructionsButton = Button.builder(
                        localized("button", "instructions"), (button) -> {
                        }
                )
                .tooltip(Tooltip.create(localized("button", "instructions.tooltip.1")
                        .append("\n\n")
                        .append(localized("button", "instructions.tooltip.2"))
                        .append("\n\n")
                        .append(localized("button", "instructions.tooltip.3"))
                        .append("\n\n")
                        .append(localized("button", "instructions.tooltip.4"))))
                .pos(x, movingY)
                .size(width, height)
                .build();
        instructionsButton.active = false;
        addRenderableWidget(instructionsButton);
        movingY += 21;

        // Toggle the layout status of all buttons
        Button toggleAllButton = Button.builder(
                        localized("button", "toggleAll"), (button) -> {
                            boolean status = buttons.stream().noneMatch((b) -> b.active);
                            buttons.forEach((b) -> b.active = status);
                        }
                )
                .tooltip(Tooltip.create(localized("button", "toggleAll.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(toggleAllButton);
        movingY += 21;

        // Move the button to the default position
        Button moveToDefaultButton = Button.builder(
                        localized("button", "moveToDefault"), (button) -> {
                            Vec2i before = buttons.getFirst().offset;
                            buttons.getFirst().offset = options().layoutOffset;
                            repositionButtons(buttons.getFirst(), before);
                        }
                )
                .tooltip(Tooltip.create(localized("button", "moveToDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(moveToDefaultButton);
        movingY += 21;

        // Split the current config off the parent class key
        Button splitConfigButton = Button.builder(
                        localized("button", "splitConfig"),
                        (button) -> Minecraft.getInstance().setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        options().buttonLayouts.put(
                                                lowestLayoutKey, new ButtonLayout(
                                                        lowestLayoutKey,
                                                        buttons.getFirst().offset,
                                                        buttons.getFirst().active,
                                                        buttons.get(1).active,
                                                        buttons.get(2).active
                                                )
                                        );
                                        Config.save();
                                        init();
                                    }
                                    Minecraft.getInstance().setScreen(this);
                                },
                                localized("title", "confirm.splitConfig"),
                                localized(
                                        "message",
                                        "confirm.splitConfig",
                                        Component.literal(rep.layoutKey == null
                                                ? lowestLayoutKey
                                                : rep.layoutKey
                                        ).withStyle(ChatFormatting.GOLD),
                                        Component.literal(lowestLayoutKey)
                                                .withStyle(ChatFormatting.GOLD)
                                )
                        ))
                )
                .tooltip(Tooltip.create(localized("button", "splitConfig.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        splitConfigButton.active = rep.layoutKey != null && !rep.layoutKey.equals(lowestLayoutKey);
        addRenderableWidget(splitConfigButton);
        movingY += 21;

        // Save the current position as default
        Button saveAsDefaultButton = Button.builder(
                        localized("button", "saveAsDefault"),
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
                .tooltip(Tooltip.create(localized("button", "saveAsDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(saveAsDefaultButton);
        movingY += 21;

        // Re-generates the screen to undo all changes made since opening
        Button undoChangesButton = Button.builder(
                        localized("button", "undoChanges"),
                        (button) -> init()
                )
                .tooltip(Tooltip.create(localized("button", "undoChanges.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(undoChangesButton);
        movingY += 21;

        // Open group selector screen
        Button reselectButton = Button.builder(
                        localized("button", "reselect"),
                        (button) -> {
                            onClose();
                            Minecraft.getInstance().setScreen(
                                    new GroupSelectorScreen(underlay, this)
                            );
                        }
                )
                .tooltip(Tooltip.create(localized("button", "reselect.tooltip")))
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

        // Safety net
        if (buttons.isEmpty())
            return;

        // Render trace lines
        drawLineFor(graphics, buttons.getFirst());

        // Render info lines
        Vec2i offset = buttons.getFirst().offset;
        graphics.drawString(
                font,
                localized("info", "offset", offset.x(), offset.y()).getString(),
                105,
                height - (font.lineHeight + 1) * 3,
                0xFFFFFFFF
        );
        graphics.drawString(
                font,
                localized(
                        "info",
                        "layoutKey.current",
                        rep.layoutKey == null ? localized("info", "layoutKey.unset") : rep.layoutKey
                ),
                105,
                height - (font.lineHeight + 1) * 2,
                0xFFFFFFFF
        );
        graphics.drawString(
                font,
                localized("info", "layoutKey.menu", lowestLayoutKey),
                105,
                height - (font.lineHeight + 1),
                0xFFFFFFFF
        );

        // Render editable widgets again, above background blur
        for (ControlButton cb : buttons) {
            cb.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Draws a horizontal and a vertical line to trace this widget back to its positional origin
     * point.
     */
    private void drawLineFor(GuiGraphics graphics, ControlButton button) {
        graphics.hLine(button.getX() - button.offset.x(), button.getX(), button.getY(), 0xFFBBBBBB);
        graphics.vLine(
                button.getX() - button.offset.x(),
                button.getY() - button.offset.y(),
                button.getY(),
                0xFFBBBBBB
        );
    }

    /**
     * Closes this screen and shows {@link PositionEditScreen#lastScreen} instead.
     */
    @Override
    public void onClose() {
        super.onClose();
        lastScreen.init(Minecraft.getInstance(), width, height);
        Minecraft.getInstance().setScreen(lastScreen);
    }

    /**
     * Saves any altered values, then calls {@link PositionEditScreen#onClose}.
     */
    public void saveAndClose() {
        String layoutKey = rep.layoutKey == null ? lowestLayoutKey : rep.layoutKey;
        boolean anyActive = false;
        for (ControlButton button : buttons) {
            button.savePolicyState();
            if (button.active)
                anyActive = true;
        }
        if (anyActive || options().buttonLayouts.containsKey(layoutKey)) {
            options().buttonLayouts.put(
                    layoutKey, new ButtonLayout(
                            layoutKey,
                            buttons.getFirst().offset,
                            buttons.getFirst().active,
                            buttons.get(1).active,
                            buttons.get(2).active
                    )
            );
        }
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
            for (ControlButton cb : buttons) {
                if (cb.isMouseOver(mouseX, mouseY)) {
                    cb.mouseClicked(mouseX, mouseY, mouseButton);
                    rep = cb;
                    dragging = true;
                    return true;
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
     * If {@code before} does not match the current offset of {@code button}, moves all other
     * widgets to match how {@code button} was moved.
     */
    private void repositionButtons(ControlButton button, Vec2i before) {
        if (!button.offset.equals(before)) {
            Vec2i diff = button.offset.subtract(before);
            for (ControlButton cb : buttons) {
                if (cb != button) {
                    cb.offset = cb.offset.add(diff);
                }
            }
        }
    }
}
