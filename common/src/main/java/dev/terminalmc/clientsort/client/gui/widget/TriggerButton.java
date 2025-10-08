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
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Operation;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.gui.screen.edit.ContainerEditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.EditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.PlayerEditorScreen;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractWidgetAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static dev.terminalmc.clientsort.util.Localization.localized;

public abstract class TriggerButton extends Button {

    public static final int WIDTH = 13;
    public static final int HEIGHT = 13;
    public static final int HALF_WIDTH = WIDTH / 2;
    public static final int HALF_HEIGHT = HEIGHT / 2;

    private final AbstractContainerScreen<?> screen;
    public final Container container;
    public final Slot referenceSlot;
    public final boolean isPlayerInv;

    private final WidgetSprites sprites;
    private final Component name;

    public final @Nullable String activePolicyKey;
    public final String lowestPolicyKey;

    public Vec2i offset;
    public boolean operationAllowed;

    protected TriggerButton(
            AbstractContainerScreen<?> screen,
            Container container,
            Slot referenceSlot,
            boolean isPlayerInv,
            WidgetSprites sprites,
            Component name,
            @Nullable String activePolicyKey,
            String lowestPolicyKey,
            Vec2i offset,
            boolean operationAllowed,
            boolean active,
            OnPress onPress
    ) {
        super(
                ((AbstractContainerScreenAccessor) screen).clientsort$getLeftPos()
                        + ((AbstractContainerScreenAccessor) screen).clientsort$getImageWidth()
                        + offset.x(),
                ((AbstractContainerScreenAccessor) screen).clientsort$getTopPos()
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
        this.referenceSlot = referenceSlot;
        this.isPlayerInv = isPlayerInv;
        this.sprites = sprites;
        this.offset = offset;
        this.activePolicyKey = activePolicyKey;
        this.lowestPolicyKey = lowestPolicyKey;
        this.operationAllowed = operationAllowed;
        this.active = active;
        this.name = name;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isMouseOver(mouseX, mouseY)) {
            boolean rightClick = mouseButton == InputConstants.MOUSE_BUTTON_RIGHT;
            if (Minecraft.getInstance().screen instanceof EditorScreen) {
                if (rightClick) {
                    if (Screen.hasShiftDown()) {
                        operationAllowed = !operationAllowed;
                    } else {
                        active = !active;
                    }
                }
                return true;
            } else if (rightClick) {
                openEditScreen();
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
        Minecraft.getInstance().setScreen(
                isPlayerInv
                        ? new PlayerEditorScreen(screen, this)
                        : new ContainerEditorScreen(screen, this)
        );
    }

    @Override
    public void renderWidget(
            @NotNull GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        AbstractContainerScreenAccessor acs = (AbstractContainerScreenAccessor) screen;

        // Keep visible
        int newX = Math.clamp(
                acs.clientsort$getLeftPos() + acs.clientsort$getImageWidth() + offset.x(),
                0,
                screen.width - WIDTH
        );
        int newY = Math.clamp(
                acs.clientsort$getTopPos() + Math.max(0, referenceSlot.y) + offset.y(),
                0,
                screen.height - HEIGHT
        );
        setX(newX);
        setY(newY);

        // Draw texture
        ResourceLocation texture = sprites.get(isActive(), isHoveredOrFocused());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), width, height);

        // Draw policy state indicator
        if (!operationAllowed) {
            graphics.hLine(getX(), getX() + width - 1, getY() + height / 2, 0xFFFF0000);
        }

        // Refresh tooltip
        if (isMouseOver(mouseX, mouseY)) {
            if (Minecraft.getInstance().screen instanceof EditorScreen
                    && ((AbstractWidgetAccessor) this).clientsort$getTooltip().get() == null) {
                Component visibilityStatus = localized(
                        "editor", active ? "enabled" : "disabled")
                        .withStyle(active
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED);
                Component operationStatus = localized(
                        "editor", operationAllowed ? "enabled" : "disabled")
                        .withStyle(operationAllowed
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED);
                setTooltip(Tooltip.create(Component.empty().append(name)
                        .append("\n")
                        .append(localized("editor", "visibility", visibilityStatus))
                        .append("\n")
                        .append(localized("editor", "operation", operationStatus))));
            } else {
                setTooltip(null);
            }
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (Minecraft.getInstance().screen instanceof EditorScreen) {
            AbstractContainerScreenAccessor acs = (AbstractContainerScreenAccessor) screen;
            int newX = Math.clamp((int) mouseX - HALF_WIDTH, 0, screen.width - WIDTH);
            int newY = Math.clamp((int) mouseY - HALF_HEIGHT, 0, screen.height - HEIGHT);

            offset = new Vec2i(
                    newX - (acs.clientsort$getLeftPos() + acs.clientsort$getImageWidth()),
                    newY - (acs.clientsort$getTopPos()
                            + Math.clamp(referenceSlot.y, 0, screen.height))
            );
        } else {
            super.onDrag(mouseX, mouseY, dragX, dragY);
        }
    }

    /**
     * Disables keyboard navigation.
     */
    @Override
    public ComponentPath nextFocusPath(@NotNull FocusNavigationEvent event) {
        return null;
    }

    /**
     * Disables focusing on click.
     */
    @Override
    public void setFocused(boolean focused) {
        if (!focused)
            super.setFocused(false);
    }

    public abstract boolean getPolicyStatus(ClassPolicy policy);

    public abstract void savePolicy(
            @Nullable Vec2i offset,
            @Nullable Operation autoOp,
            boolean autoOpOther,
            Collection<Integer> slots
    );
}
