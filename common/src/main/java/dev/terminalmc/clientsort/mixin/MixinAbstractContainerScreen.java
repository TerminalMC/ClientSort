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

package dev.terminalmc.clientsort.mixin;

import com.google.common.base.Suppliers;
import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.inventory.ContainerScreenHelper;
import dev.terminalmc.clientsort.inventory.sort.InventorySorter;
import dev.terminalmc.clientsort.inventory.sort.SortOrder;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.SoundManager;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;
import java.util.function.Supplier;

import static dev.terminalmc.clientsort.config.Config.options;

/**
 * Enables sorting via mouseclick or keypress.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen {
    
    protected MixinAbstractContainerScreen(Component title) {
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

    /**
     * Supplies a {@link ContainerScreenHelper} for this screen.
     */
    @SuppressWarnings("unchecked")
    @Unique
    private final Supplier<ContainerScreenHelper<AbstractContainerScreen<AbstractContainerMenu>>>
            clientSort$screenHelper = Suppliers.memoize(
            () -> ContainerScreenHelper.of(
                    (AbstractContainerScreen<AbstractContainerMenu>) (Object) this,
                    (slot, data, slotActionType, sound) ->
                            new InteractionManager.CallbackEvent(() -> {
                                slotClicked(slot, ((ISlot) slot).clientSort$getIdInContainer(),
                                        data, slotActionType);
                                if (sound) SoundManager.play();
                                return InteractionManager.TICK_WAITER;
                            })
            )
    );

    /**
     * Allows triggering sort via mouse click.
     */
    @Inject(
            method = "mouseClicked",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onMouseClicked(double mouseX, double mouseY, int button,
                                CallbackInfoReturnable<Boolean> cir) {
        if (clientSort$shouldSort((keyMapping) -> keyMapping.matchesMouse(button))) {
            if (clientSort$triggerSort()) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    /**
     * Allows triggering sort via key press.
     */
    @Inject(
            method = "keyPressed",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onKeyPressed(int keyCode, int scanCode, int modifiers,
                              CallbackInfoReturnable<Boolean> cir) {
        if (clientSort$shouldSort((keyMapping) -> keyMapping.matches(keyCode, scanCode))) {
            if (clientSort$triggerSort()) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    /**
     * @return {@code true} if the specified input should trigger sorting and
     * also should not trigger a vanilla operation.
     */
    @SuppressWarnings("ConstantConditions")
    @Unique
    private boolean clientSort$shouldSort(Function<KeyMapping, Boolean> inputMatcher) {
        // Check that we're hovering a slot
        if (hoveredSlot == null) return false;
        
        // Check that the input matches the sort key
        if (!inputMatcher.apply(ClientSort.SORT_KEY)) return false;
        
        // Check that the input will not trigger a vanilla operation
        Options options = this.minecraft.options;
        // Pick
        if (((inputMatcher.apply(options.keyPickItem) 
            && this.minecraft.gameMode.hasInfiniteItems()
            && (this.hoveredSlot.hasItem()
                || !this.draggingItem.isEmpty()
                || !this.menu.getCarried().isEmpty())))) {
            return false;
        }
        // Drop
        if (inputMatcher.apply(options.keyDrop) && this.hoveredSlot.hasItem()) return false;
        // Offhand swap
        if (inputMatcher.apply(options.keySwapOffhand)) return false;
        // Hotbar swap
        for (int i = 0; i < 9; i++) {
            if (inputMatcher.apply(options.keyHotbarSlots[i])) return false;
        }
        // No operations
        return true;
    }

    /**
     * Triggers sorting of this screen's inventory.
     * @return {@code true} if sorting was completed.
     */
    @Unique
    @SuppressWarnings("ConstantConditions")
    public boolean clientSort$triggerSort() {
        if (hoveredSlot == null) return false;

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
            InventorySorter sorter = new InventorySorter(clientSort$screenHelper.get(),
                    (AbstractContainerScreen<?>) (Object) this, hoveredSlot);
            sorter.sort(sortOrder);
            return true;
        }
        return false;
    }
}
