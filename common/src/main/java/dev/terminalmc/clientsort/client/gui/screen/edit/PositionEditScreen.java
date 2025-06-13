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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public abstract class PositionEditScreen extends Screen {
    private final Screen lastScreen;
    private final Screen underlay;
    private final LinkedList<ControlButton> buttons = new LinkedList<>();

    private ControlButton selected;
    private boolean dragging;

    private @Nullable CycleButton<Boolean> statusButton = null;

    public PositionEditScreen(Screen underlay, ControlButton button) {
        this(underlay, button, underlay);
    }

    public PositionEditScreen(Screen underlay, ControlButton button, Screen lastScreen) {
        super(localized("title", "positionEditor"));
        this.font = Minecraft.getInstance().font;
        this.lastScreen = lastScreen;
        this.underlay = underlay;
        this.selected = button;
    }

    /**
     * Re-initializes {@link PositionEditScreen#underlay}, then this screen's
     * GUI elements.
     */
    @Override
    public void init() {
        super.init();

        if (Minecraft.getInstance().screen != this) {
            Minecraft.getInstance().setScreen(this);
        }

        underlay.init(Minecraft.getInstance(), width, height);

        reloadButtons();
        setSelected(buttons.getFirst());

        rebuildGui();
    }

    /**
     * Reloads the list of editable buttons.
     */
    private void reloadButtons() {
        buttons.clear();
        buttons.addAll(getButtons());

        // Should never happen, but worth catching anyway
        if (buttons.size() != 3) {
            ClientSort.LOG.error("Failed to reload buttons on PositionEditScreen: "
                    + "Button list is too small or does not contain provided button");
            onClose();
            Minecraft.getInstance().setScreen(underlay);
        }
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

        MultiLineTextWidget messageWidget = new MultiLineTextWidget(
                20,
                2 + font.lineHeight,
                localized("message", "positionEditor"),
                font
        );
        messageWidget.setMaxWidth(width - 40);
        messageWidget.setCentered(true);
        addRenderableWidget(messageWidget);

        int numButtons = 9;
        int x = 2;
        int movingY = height - 21 * numButtons;
        int width = 100;
        int height = 20;

        // Toggle the status of the selected button
        statusButton = CycleButton.onOffBuilder(selected.active)
                .create(x, movingY, width, height, localized("button", "status"),
                        (button, status) -> selected.active = status);
        addRenderableWidget(statusButton);
        movingY += 21;

        // Toggle the status of all buttons
        Button toggleAllButton = Button.builder(localized("button", "toggleAll"),
                        (button) -> {
                            boolean status = buttons.stream().noneMatch((b) -> b.active);
                            buttons.forEach((b) -> b.active = status);
                        })
                .tooltip(Tooltip.create(localized("button", "toggleAll.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(toggleAllButton);
        movingY += 21;

        // Move the button to the default position
        Button moveToDefaultButton = Button.builder(localized("button", "moveToDefault"),
                        (button) -> {
                            Vec2i before = buttons.getFirst().offset;
                            buttons.getFirst().offset = options().buttonDefaultOffset;
                            repositionButtons(buttons.getFirst(), before);
                        })
                .tooltip(Tooltip.create(localized("button", "moveToDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(moveToDefaultButton);
        movingY += 21;

        // Split the current config off the parent class key
        String containerName = selected.container.getClass().getName();
        Button splitConfigButton = Button.builder(localized("button", "splitConfig"),
                        (button) -> Minecraft.getInstance().setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        options().buttonLayouts.put(
                                                containerName,
                                                new ButtonLayout(
                                                        containerName,
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
                                localized("message", "confirm.splitConfig", selected.layoutKey,
                                        containerName)
                        )))
                .tooltip(Tooltip.create(localized("button", "splitConfig.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        splitConfigButton.active = !containerName.equals(selected.layoutKey);
        addRenderableWidget(splitConfigButton);
        movingY += 21;

        // Save the current position as default
        Button saveAsDefaultButton = Button.builder(localized("button", "saveAsDefault"),
                        (button) -> Minecraft.getInstance().setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        options().buttonDefaultOffset = buttons.getFirst().offset;
                                        Config.save();
                                        init();
                                    }
                                    Minecraft.getInstance().setScreen(this);
                                },
                                localized("title", "confirm.saveAsDefault"),
                                localized("message", "confirm.saveAsDefault")
                        )))
                .tooltip(Tooltip.create(localized("button", "saveAsDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(saveAsDefaultButton);
        movingY += 21;

        // Undo all changes made since opening this screen
        Button undoChangesButton = Button.builder(localized("button", "undoChanges"),
                        (button) -> init())
                .tooltip(Tooltip.create(localized("button", "undoChanges.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(undoChangesButton);
        movingY += 21;

        // Open group selector screen
        Button reselectButton = Button.builder(localized("button", "reselect"),
                        (button) -> {
                            onClose();
                            Minecraft.getInstance().setScreen(new GroupSelectorScreen(underlay, this));
                        })
                .tooltip(Tooltip.create(localized("button", "reselect.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(reselectButton);
        movingY += 21;

        // Close this screen without saving
        Button cancelButton = Button.builder(CommonComponents.GUI_CANCEL,
                        (button) -> onClose())
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(cancelButton);
        movingY += 21;

        // Save all changes then close this screen
        Button doneButton = Button.builder(CommonComponents.GUI_DONE,
                        (button) -> saveAndClose())
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

        // Render trace lines
        drawLineFor(graphics, buttons.getFirst());

        // Render info lines
        Vec2i offset = buttons.getFirst().offset;
        graphics.drawString(
                font,
                localized("info", "offset", offset.x(), offset.y()).getString(),
                105,
                height - (font.lineHeight * 2 + 2),
                0xFFFFFFFF
        );
        graphics.drawString(
                font,
                localized("info", "className", selected.container.getClass().getName()),
                105,
                height - (font.lineHeight + 1),
                0xFFFFFFFF
        );

        // Render editable widgets above background blur
        for (ControlButton cb : buttons) {
            cb.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Modifies the background blur to be constant irrespective of the
     * configured value.
     * <p>
     * Minimal blur is used to prevent the editable widgets disappearing under
     * underlay items on a higher render layer, while still keeping the underlay
     * detail discernible.
     */
    @Override
    protected void renderBlurredBackground(float partialTick) {
        int original = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(1);
        super.renderBlurredBackground(partialTick);
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(original);
    }

    /**
     * Draws a horizontal and a vertical line to trace this widget back to its
     * positional origin point.
     */
    private void drawLineFor(GuiGraphics graphics, ControlButton button) {
        graphics.hLine(
                button.getX() - button.offset.x(),
                button.getX(),
                button.getY(),
                0xFFFF0000
        );
        graphics.vLine(
                button.getX() - button.offset.x(),
                button.getY() - button.offset.y(),
                button.getY(),
                0xFFFF0000
        );
    }

    /**
     * Closes this screen and shows {@link PositionEditScreen#lastScreen}
     * instead.
     */
    @Override
    public void onClose() {
        super.onClose();
        underlay.init(Minecraft.getInstance(), width, height);
        if (lastScreen != underlay) {
            lastScreen.init(Minecraft.getInstance(), width, height);
        }
        Minecraft.getInstance().setScreen(lastScreen);
    }

    /**
     * Saves any altered values, then closes this screen and shows
     * {@link PositionEditScreen#lastScreen} instead.
     */
    public void saveAndClose() {
        options().buttonLayouts.put(selected.layoutKey, new ButtonLayout(
                selected.layoutKey,
                buttons.getFirst().offset,
                buttons.getFirst().active,
                buttons.get(1).active,
                buttons.get(2).active
        ));
        Config.save();
        onClose();
    }

    /**
     * Allows pressing the arrow keys to reposition the selected widget.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int distance = Screen.hasShiftDown() ? 6 : 1;
        @Nullable Vec2i movement = switch(keyCode) {
            case InputConstants.KEY_LEFT -> new Vec2i(-distance, 0);
            case InputConstants.KEY_RIGHT -> new Vec2i(distance, 0);
            case InputConstants.KEY_UP -> new Vec2i(0, -distance);
            case InputConstants.KEY_DOWN -> new Vec2i(0, distance);
            default -> null;
        };
        if (movement != null) {
            Vec2i before = selected.offset;
            selected.offset = selected.offset.add(movement);
            repositionButtons(selected, before);
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
                if (isMouseOver(cb, mouseX, mouseY)) {
                    cb.mouseClicked(mouseX, mouseY, mouseButton);
                    setSelected(cb);
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            Vec2i before = selected.offset;
            if (selected.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                repositionButtons(selected, before);
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
     * Selects {@code widget} and marks it as focused.
     */
    public void setSelected(@NotNull ControlButton widget) {
        if (widget != selected) {
            selected.setFocused(false);
        }
        selected = widget;
        selected.setFocused(true);
        if (statusButton != null) statusButton.setValue(selected.active);
    }

    /**
     * A status-agnostic alternative to {@link AbstractWidget#isMouseOver}.
     * @return {@code true} if the mouse position overlaps {@code widget}.
     */
    public boolean isMouseOver(AbstractWidget widget, double mouseX, double mouseY) {
        return mouseX >= (double)widget.getX() &&
                mouseY >= (double)widget.getY() &&
                mouseX < (double)(widget.getX() + widget.getWidth())
                && mouseY < (double)(widget.getY() + widget.getHeight());
    }

    /**
     * If {@code before} does not match the current offset of {@code button},
     * moves all other widgets to match how {@code button} was moved.
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
