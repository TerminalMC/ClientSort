/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.mixin.client;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.gui.screen.edit.EditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.SelectorScreen;
import dev.terminalmc.clientsort.client.inventory.helper.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;
import java.util.function.Supplier;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Enables triggering inventory operations via mouseclick or keypress.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    public abstract T getMenu();

    /**
     * When a client-side survival inventory interaction operation is triggered by a button click
     * outside the inventory, the game may detect the click and erroneously throw the carried item.
     * To prevent this, a flag is used to block all outside clicks while an operation is in
     * progress.
     */
    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beforeSlotClicked(
            Slot slot,
            int slotId,
            int mouseButton,
            ContainerInput input,
            CallbackInfo ci
    ) {
        if (slotId < 0 && ClientSort.operatingClient) {
            ci.cancel();
        }
    }

    /**
     * Allows triggering operations via mouse click.
     */
    @Inject(
            method = "mouseClicked",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void beforeMouseClicked(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Supplier<Boolean> op =
                clientsort$getOperation((keyMapping) -> keyMapping.matchesMouse(event));
        if (op != null && op.get()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Allows triggering operations via key press.
     */
    @Inject(
            method = "keyPressed",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void beforeKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        Supplier<Boolean> op =
                clientsort$getOperation((keyMapping) -> keyMapping.matches(event));
        if (op != null && op.get()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * @return {@code true} if the input should trigger a mod operation and also should not trigger
     * a vanilla operation.
     */
    @SuppressWarnings("ConstantConditions")
    @Unique
    private @Nullable Supplier<Boolean> clientsort$getOperation(
            Function<KeyMapping, Boolean> inputMatcher
    ) {
        // If no hovered slot, edit or nothing
        boolean isEditKey = inputMatcher.apply(KeybindManager.EDIT_KEY);
        if (hoveredSlot == null) {
            return isEditKey ? this::clientsort$openEditor : null;
        }

        // Check that the input will not trigger a vanilla operation
        Options options = this.minecraft.options;
        // Pick item
        if (((inputMatcher.apply(options.keyPickItem)
                && this.minecraft.player.hasInfiniteMaterials()
                && (this.hoveredSlot.hasItem()
                || !this.menu.getCarried().isEmpty())))) {
            return null;
        }
        // Drop slot
        if (inputMatcher.apply(options.keyDrop) && this.hoveredSlot.hasItem())
            return null;
        // Swap with offhand
        if (inputMatcher.apply(options.keySwapOffhand))
            return null;
        // Swap with hotbar
        for (int i = 0; i < 9; i++) {
            if (inputMatcher.apply(options.keyHotbarSlots[i]))
                return null;
        }

        // No vanilla operations; trigger mod operation
        if (isEditKey) {
            return this::clientsort$openEditor;
        } else if (inputMatcher.apply(KeybindManager.SORT_KEY)) {
            return this::clientsort$sort;
        } else if (inputMatcher.apply(KeybindManager.STACK_FILL_KEY)) {
            return this::clientsort$fillStacks;
        } else if (inputMatcher.apply(KeybindManager.MATCH_TRANSFER_KEY)) {
            return this::clientsort$transferMatching;
        } else if (inputMatcher.apply(KeybindManager.TRANSFER_KEY)) {
            return this::clientsort$transfer;
        } else {
            return null;
        }
    }

    @Unique
    private boolean clientsort$openEditor() {
        Minecraft.getInstance()
                .gui.setScreen(new SelectorScreen((AbstractContainerScreen<?>) (Object) this));
        return true;
    }

    @Unique
    private boolean clientsort$sort() {
        if (hoveredSlot == null)
            return false;

        SortOrder sortOrder = options().sortOrder;
        if (KeybindManager.hasShiftDown()) {
            sortOrder = options().shiftSortOrder;
        } else if (KeybindManager.hasControlDown()) {
            sortOrder = options().ctrlSortOrder;
        } else if (KeybindManager.hasAltDown()) {
            sortOrder = options().altSortOrder;
        }

        return SingleUseOperator.sort(
                (AbstractContainerScreen<?>) (Object) this,
                hoveredSlot,
                false,
                sortOrder
        );
    }

    @Unique
    private boolean clientsort$fillStacks() {
        if (hoveredSlot == null)
            return false;
        return SingleUseOperator.fillStacks(
                (AbstractContainerScreen<?>) (Object) this,
                hoveredSlot,
                false
        );
    }

    @Unique
    private boolean clientsort$transferMatching() {
        if (hoveredSlot == null)
            return false;
        return SingleUseOperator.transferMatching(
                (AbstractContainerScreen<?>) (Object) this,
                hoveredSlot,
                false
        );
    }

    @Unique
    private boolean clientsort$transfer() {
        if (hoveredSlot == null)
            return false;
        return SingleUseOperator.transfer(
                (AbstractContainerScreen<?>) (Object) this,
                hoveredSlot,
                false
        );
    }

    /**
     * Renders warning and debug overlays.
     */
    @Inject(
            method = "extractRenderState",
            at = @At("TAIL")
    )
    private void afterRender(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!this.equals(Minecraft.getInstance().gui.screen()))
            return;

        if (ClientSort.overlayMessage != null) {
            graphics.pose().pushMatrix();
            ClientSort.overlayMessage.extractWidgetRenderState(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
            graphics.pose().popMatrix();
        }

        if (!debug())
            return;

        ContainerScreenHelper<?> helper =
                ContainerScreenHelper.of((AbstractContainerScreen<?>) (Object) this);

        float scale = 0.7F;
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        for (Slot slot : menu.slots) {
            int slotId = ((ISlot) slot).clientsort$getIndexInMenu();
            int slotIdx = ((ISlot) slot).clientsort$getIndexInContainer();

            // Draw disabled indicator, top left
            if (!(Minecraft.getInstance().gui.screen() instanceof EditorScreen)) {
                Object object = getObj(slot, getMenu());
                if (object == null)
                    continue;
                boolean isPlayerInv = slot.container instanceof Inventory;
                Component invTitle = isPlayerInv
                        ? ((AbstractContainerScreenAccessor) this).clientsort$getPlayerInventoryTitle()
                        : this.getTitle();
                @Nullable ClassPolicy policy =
                        PolicyManager.getPolicy(object.getClass(), invTitle.getString());
                if (policy != null && policy.ignoredSlots().contains(slotIdx)) {
                    //noinspection UnnecessaryUnicodeEscape
                    graphics.text(
                            Minecraft.getInstance().font,
                            "\u274C",
                            (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getLeftPos()
                                    + slot.x)
                                    / scale),
                            (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getTopPos()
                                    + slot.y)
                                    / scale),
                            0xFFFF0000
                    );
                }
            }

            // Draw slot ID, bottom left
            graphics.text(
                    Minecraft.getInstance().font,
                    String.valueOf(KeybindManager.hasShiftDown() ? slotIdx : slotId),
                    (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getLeftPos()
                            + slot.x)
                            / scale),
                    (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getTopPos()
                            + slot.y + 12)
                            / scale),
                    0xFFFFFFFF
            );

            // Draw slot scope, bottom right
            graphics.text(
                    Minecraft.getInstance().font,
                    String.valueOf(helper.getScope(slot).ordinal()),
                    (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getLeftPos()
                            + slot.x + 12)
                            / scale),
                    (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getTopPos()
                            + slot.y + 12)
                            / scale),
                    0xFFFFFFFF
            );
        }

        graphics.pose().popMatrix();
    }
}
