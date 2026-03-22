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
import dev.terminalmc.clientsort.mixin.client.accessor.GuiGraphicsExtractorAccessor;
import dev.terminalmc.clientsort.mixin.client.accessor.GuiRenderStateAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
        this.underlay = underlay;
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        super.init();
        underlay.init(width, height);
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

        CycleButton<@NotNull Boolean> toggleButton =
                CycleButton.booleanBuilder(
                        localized("editor", "enabled").withStyle(ChatFormatting.GREEN),
                        localized("editor", "disabled").withStyle(ChatFormatting.RED),
                        options().showButtons
                ).create(
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
    public void extractRenderState(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        underlay.extractBackground(graphics, mouseX, mouseY, partialTick);
        underlay.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Workaround for other mods adding blur when rendering the underlay
        ((GuiRenderStateAccessor) ((GuiGraphicsExtractorAccessor) graphics).clientsort$getGuiRenderState())
                .clientsort$setFirstStratumAfterBlur(Integer.MAX_VALUE);
        graphics.nextStratum();
        extractBlurredBackground(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 2, 0xFFFFFFFF);

        if (options().showButtons) {
            for (TriggerButton cb : buttons) {
                cb.extractContents(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * Removes the call to {@link Screen#extractBlurredBackground}, since we add a call in
     * {@link SelectorScreen#extractRenderState} and the method can only be called once.
     */
    @Override
    public void extractBackground(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (Minecraft.getInstance().level == null) {
            extractPanorama(graphics, partialTick);
        }
        extractMenuBackground(graphics);
    }

    @Override
    protected void extractBlurredBackground(@NotNull GuiGraphicsExtractor graphics) {
        // Heavy blur, we want the widgets to really stand out
        int original = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(6);
        super.extractBlurredBackground(graphics);
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(original);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (lastScreen instanceof EditorScreen pes && !options().showButtons) {
            pes.onClose();
        } else {
            lastScreen.init(width, height);
            Minecraft.getInstance().setScreen(lastScreen);
        }
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        } else {
            for (TriggerButton cb : buttons) {
                if (cb.isMouseOver(event.x(), event.y())) {
                    cb.playDownSound(Minecraft.getInstance().getSoundManager());
                    onClose();
                    cb.openEditScreen();
                }
            }
            return false;
        }
    }
}
