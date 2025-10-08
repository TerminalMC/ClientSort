/*
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

package dev.terminalmc.clientsort.client.gui.screen.edit;

import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.gui.TriggerButtonManager;
import dev.terminalmc.clientsort.client.gui.widget.TriggerButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public class SelectorScreen extends Screen {

    private final Screen lastScreen;
    private final AbstractContainerScreen<?> underlay;
    private final LinkedList<TriggerButton> buttons = new LinkedList<>();

    public SelectorScreen(AbstractContainerScreen<?> underlay) {
        this(underlay, underlay);
    }

    public SelectorScreen(AbstractContainerScreen<?> underlay, Screen lastScreen) {
        super(localized("title", "groupSelector"));
        this.font = Minecraft.getInstance().font;
        this.underlay = underlay;
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        super.init();
        underlay.init(Minecraft.getInstance(), width, height);
        reloadButtons();
        rebuildGui();
    }

    private void reloadButtons() {
        buttons.clear();
        buttons.addAll(TriggerButtonManager.getContainerButtons());
        buttons.addAll(TriggerButtonManager.getPlayerButtons());
    }

    private void rebuildGui() {
        clearWidgets();

        StringWidget titleWidget = new StringWidget(0, 2, width, font.lineHeight, title, font);
        addRenderableWidget(titleWidget);

        CycleButton<Boolean> toggleButton =
                CycleButton.booleanBuilder(
                        localized("editor", "enabled").withStyle(ChatFormatting.GREEN),
                        localized("editor", "disabled").withStyle(ChatFormatting.RED)
                ).withInitialValue(options().showButtons).create(
                        width / 2 - 125,
                        height - 22,
                        120,
                        20,
                        localized("editor", "buttons"),
                        (buttons, status) -> {
                            options().showButtons = status;
                            Config.save();
                            init();
                        }
                );
        addRenderableWidget(toggleButton);

        Button cancelButton = Button.builder(CommonComponents.GUI_BACK, (button) -> onClose())
                .pos(width / 2 + 5, height - 22)
                .size(120, 20)
                .build();
        addRenderableWidget(cancelButton);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        underlay.render(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (options().showButtons) {
            for (TriggerButton cb : buttons) {
                cb.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    protected void renderBlurredBackground() {
        // Heavy blur, we want the widgets to really stand out
        int original = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(6);
        super.renderBlurredBackground();
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(original);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (lastScreen instanceof EditorScreen pes && !options().showButtons) {
            pes.onClose();
        } else {
            lastScreen.init(Minecraft.getInstance(), width, height);
            Minecraft.getInstance().setScreen(lastScreen);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        } else {
            for (TriggerButton cb : buttons) {
                if (cb.isMouseOver(mouseX, mouseY)) {
                    cb.playDownSound(Minecraft.getInstance().getSoundManager());
                    onClose();
                    cb.openEditScreen();
                }
            }
            return false;
        }
    }
}
