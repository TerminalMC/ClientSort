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

package dev.terminalmc.clientsort.client.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.gui.screen.edit.ContainerPositionEditScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.PlayerPositionEditScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.PositionEditScreen;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

public abstract class ControlButton extends Button {
    public static final int WIDTH = 13;
    public static final int HEIGHT = 13;
    public static final int HALF_WIDTH = WIDTH / 2;
    public static final int HALF_HEIGHT = HEIGHT / 2;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ClientSort.MOD_ID, "textures/gui.png");

    private final Screen screen;
    public final Container container;
    public final String layoutKey;
    final boolean isPlayerInv;
    private final Slot referenceSlot;
    private final Vec2i spriteOffset;

    public Vec2i offset;

    protected ControlButton(
            AbstractContainerScreen<?> screen,
            Container container,
            String layoutKey,
            boolean isPlayerInv,
            Slot referenceSlot,
            Vec2i spriteOffset,
            Vec2i offset,
            OnPress onPress,
            boolean active
    ) {
        super(
                ((AbstractContainerScreenAccessor) screen).getLeftPos()
                        + ((AbstractContainerScreenAccessor) screen).getImageWidth()
                        + offset.x(),
                ((AbstractContainerScreenAccessor) screen).getTopPos()
                        + referenceSlot.y
                        + offset.y(),
                WIDTH,
                HEIGHT,
                CommonComponents.EMPTY,
                onPress,
                DEFAULT_NARRATION
        );
        this.screen = screen;
        this.container = container;
        this.layoutKey = layoutKey;
        this.isPlayerInv = isPlayerInv;
        this.referenceSlot = referenceSlot;
        this.spriteOffset = spriteOffset;
        this.offset = offset;
        this.active = active;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isMouseOver(mouseX, mouseY)) {
            if (mouseButton == InputConstants.MOUSE_BUTTON_RIGHT) {
                openEditScreen();
                return true;
            } else if (Minecraft.getInstance().screen instanceof PositionEditScreen) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Checks mouse position irrespective of widget status or visibility.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= (double) getX()
                && mouseY >= (double) getY()
                && mouseX < (double) (getX() + width)
                && mouseY < (double) (getY() + height);
    }

    public void openEditScreen() {
        Minecraft.getInstance().setScreen(isPlayerInv
                ? new PlayerPositionEditScreen(screen, this)
                : new ContainerPositionEditScreen(screen, this)
        );
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        AbstractContainerScreenAccessor acs = (AbstractContainerScreenAccessor) screen;

        // Keep visible
        int newX = Mth.clamp(
                acs.getLeftPos() + acs.getImageWidth() + offset.x(),
                0,
                screen.width - WIDTH
        );
        int newY = Mth.clamp(
                acs.getTopPos() + referenceSlot.y + offset.y(),
                0,
                screen.height - HEIGHT
        );

        setX(newX);
        setY(newY);

        int u = spriteOffset.x() * WIDTH;
        int v = spriteOffset.y() * HEIGHT;
        if (!isActive()) {
            v += HEIGHT * 2;
        } else if (isHovered() || isFocused()) {
            v += HEIGHT;
        }

        graphics.blit(TEXTURE, getX(), getY(), u, v, width, height);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (Minecraft.getInstance().screen instanceof PositionEditScreen) {
            AbstractContainerScreenAccessor acs = (AbstractContainerScreenAccessor) screen;
            int newX = Mth.clamp((int) mouseX - HALF_WIDTH, 0, screen.width - WIDTH);
            int newY = Mth.clamp((int) mouseY - HALF_HEIGHT, 0, screen.height - HEIGHT);

            offset = new Vec2i(
                    newX - (acs.getLeftPos() + acs.getImageWidth()),
                    newY - (acs.getTopPos() + referenceSlot.y)
            );
        } else {
            super.onDrag(mouseX, mouseY, dragX, dragY);
        }
    }
}