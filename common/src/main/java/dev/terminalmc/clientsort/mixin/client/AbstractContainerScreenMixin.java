/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.mixin.client;

import com.google.common.base.Suppliers;
import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.gui.screen.edit.EditorScreen;
import dev.terminalmc.clientsort.client.gui.screen.edit.SelectorScreen;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import dev.terminalmc.clientsort.client.util.SoundManager;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
    private ItemStack draggingItem;

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type);

    @Shadow
    public abstract T getMenu();

    /**
     * When a client-side survival inventory interaction operation is triggered by a button click
     * outside the inventory, Minecraft may detect the click and erroneously throw the carried item.
     * To prevent this, a flag is used to block all outside clicks while an operation is in
     * progress.
     */
    @Inject(
            method = "slotClicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beforeSlotClicked(
            Slot slot,
            int slotId,
            int mouseButton,
            ClickType type,
            CallbackInfo ci
    ) {
        if (slotId < 0 && ClientSort.operatingClient) {
            ci.cancel();
        }
    }

    /**
     * Supplies a {@link ContainerScreenHelper} for this screen.
     */
    @SuppressWarnings("unchecked")
    @Unique
    private final Supplier<ContainerScreenHelper<AbstractContainerScreen<AbstractContainerMenu>>>
            clientsort$screenHelper = Suppliers.memoize(() -> ContainerScreenHelper.of(
            (AbstractContainerScreen<AbstractContainerMenu>) (Object) this,
            (slot, mouseButton, clickType, playSound) -> new InteractionManager.CallbackEvent(() -> {
                slotClicked(
                        slot,
                        ((ISlot) slot).clientsort$getIndexInMenu(),
                        mouseButton,
                        clickType
                );
                if (playSound)
                    SoundManager.play();
                return InteractionManager.TICK_WAITER;
            })
    ));

    /**
     * Allows triggering operations via mouse click.
     */
    @Inject(
            method = "mouseClicked",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void beforeMouseClicked(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Supplier<Boolean> op =
                clientsort$getOperation((keyMapping) -> keyMapping.matchesMouse(button));
        if (op != null && op.get()) {
            cir.setReturnValue(true);
            cir.cancel();
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
    private void beforeKeyPressed(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Supplier<Boolean> op =
                clientsort$getOperation((keyMapping) -> keyMapping.matches(keyCode, scanCode));
        if (op != null && op.get()) {
            cir.setReturnValue(true);
            cir.cancel();
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
        // If key is not edit key, check that we're hovering a slot
        boolean isEditKey = inputMatcher.apply(KeybindManager.EDIT_KEY);
        if (!isEditKey && hoveredSlot == null)
            return null;

        // Check that the input will not trigger a vanilla operation
        Options options = this.minecraft.options;
        // Pick item
        if (((inputMatcher.apply(options.keyPickItem)
                && this.minecraft.player.hasInfiniteMaterials()
                && (this.hoveredSlot.hasItem()
                || !this.draggingItem.isEmpty()
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
            return this::clientsort$matchTransfer;
        } else if (inputMatcher.apply(KeybindManager.TRANSFER_KEY)) {
            return this::clientsort$transfer;
        } else {
            return null;
        }
    }

    @Unique
    private boolean clientsort$openEditor() {
        Minecraft.getInstance()
                .setScreen(new SelectorScreen((AbstractContainerScreen<?>) (Object) this));
        return true;
    }

    @Unique
    private boolean clientsort$sort() {
        if (hoveredSlot == null)
            return false;

        SortOrder sortOrder;
        if (hasShiftDown()) {
            sortOrder = options().shiftSortOrder;
        } else if (hasControlDown()) {
            sortOrder = options().ctrlSortOrder;
        } else if (hasAltDown()) {
            sortOrder = options().altSortOrder;
        } else {
            sortOrder = options().sortOrder;
        }

        if (sortOrder != null && sortOrder != SortOrder.NONE) {
            SingleUseOperator<?> operator = SingleUseOperator.getOperator(
                    (AbstractContainerScreen<?>) (Object) this,
                    clientsort$screenHelper.get(),
                    hoveredSlot,
                    Operation.SORT,
                    false
            );
            if (operator != null)
                operator.trySort(sortOrder);
            return true;
        }
        return false;
    }

    @Unique
    private boolean clientsort$fillStacks() {
        SingleUseOperator<?> operator = SingleUseOperator.getOperator(
                (AbstractContainerScreen<?>) (Object) this,
                clientsort$screenHelper.get(),
                hoveredSlot,
                Operation.STACK_FILL,
                false
        );
        if (operator != null)
            operator.tryFillStacks();
        return true;
    }

    @Unique
    private boolean clientsort$matchTransfer() {
        SingleUseOperator<?> operator = SingleUseOperator.getOperator(
                (AbstractContainerScreen<?>) (Object) this,
                clientsort$screenHelper.get(),
                hoveredSlot,
                Operation.MATCH_TRANSFER,
                false
        );
        if (operator != null)
            operator.tryMatchTransfer();
        return true;
    }

    @Unique
    private boolean clientsort$transfer() {
        SingleUseOperator<?> operator = SingleUseOperator.getOperator(
                (AbstractContainerScreen<?>) (Object) this,
                clientsort$screenHelper.get(),
                hoveredSlot,
                Operation.TRANSFER,
                false
        );
        if (operator != null)
            operator.tryTransfer();
        return true;
    }

    /**
     * Displays slot numbers if debug mode is enabled.
     */
    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void afterRender(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!this.equals(Minecraft.getInstance().screen))
            return;

        if (ClientSort.overlayMessage != null) {
            graphics.pose().pushMatrix();
            ClientSort.overlayMessage.renderWidget(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popMatrix();
        }

        if (!debug())
            return;

        ContainerScreenHelper<?> helper = ContainerScreenHelper.of(
                (AbstractContainerScreen<?>) (Object) this,
                (a, b, c, d) -> null
        );

        float scale = 0.7F;
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        for (Slot slot : menu.slots) {
            int slotId = ((ISlot) slot).clientsort$getIndexInMenu();

            // Draw disabled indicator, top left
            if (!(Minecraft.getInstance().screen instanceof EditorScreen)) {
                Object object = getObj(slot, getMenu());
                if (object == null)
                    continue;
                @Nullable ClassPolicy policy = PolicyManager.getPolicy(object.getClass());
                if (policy != null && policy.ignoredSlots().contains(slotId)) {
                    //noinspection UnnecessaryUnicodeEscape
                    graphics.drawString(
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

            if (hasShiftDown()) {
                slotId = ((ISlot) slot).clientsort$getIndexInContainer();
            } else if (hasControlDown()) {
                slotId = slot.getContainerSlot();
            }
            // Draw slot ID, bottom left
            graphics.drawString(
                    Minecraft.getInstance().font,
                    String.valueOf(slotId),
                    (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getLeftPos()
                            + slot.x)
                            / scale),
                    (int) ((((AbstractContainerScreenAccessor) (this)).clientsort$getTopPos()
                            + slot.y + 12)
                            / scale),
                    0xFFFFFFFF
            );
            // Draw slot scope, bottom right
            graphics.drawString(
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
