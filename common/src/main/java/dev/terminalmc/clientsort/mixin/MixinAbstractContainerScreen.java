/*
 * Copyright 2022 Siphalor
 * Copyright 2024 TerminalMC
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
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.inventory.ContainerScreenHelper;
import dev.terminalmc.clientsort.inventory.sort.InventorySorter;
import dev.terminalmc.clientsort.inventory.sort.SortMode;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.inject.IContainerScreen;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen implements IContainerScreen {
    protected MixinAbstractContainerScreen(Component textComponent_1) {
        super(textComponent_1);
    }

    @Shadow
    protected abstract void slotClicked(Slot slot_1, int int_1, int int_2, ClickType slotActionType_1);

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    private ItemStack draggingItem;

    @Inject(
            method = "mouseClicked",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onMouseClicked(double mouseX, double mouseY, int button,
                                CallbackInfoReturnable<Boolean> cir) {
        if (this.hoveredSlot != null
                && ClientSort.SORT_KEY.matchesMouse(button)
                && !clientSort$specialOperation(button)) {
            mouseWheelie_triggerSort();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(
            method = "keyPressed",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onKeyPressed(int keyCode, int scanCode, int modifiers,
                              CallbackInfoReturnable<Boolean> cir) {
        if (this.hoveredSlot != null
                && ClientSort.SORT_KEY.matches(keyCode, scanCode)
                && !clientSort$specialOperation(keyCode, scanCode)) {
            mouseWheelie_triggerSort();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Unique
    private final Supplier<ContainerScreenHelper<AbstractContainerScreen<AbstractContainerMenu>>> clientSort$screenHelper = Suppliers.memoize(
            () -> ContainerScreenHelper.of((AbstractContainerScreen<AbstractContainerMenu>) (Object) this, (slot, data, slotActionType) -> new InteractionManager.CallbackEvent(() -> {
                slotClicked(slot, ((ISlot) slot).mouseWheelie_getIdInContainer(), data, slotActionType);
                return InteractionManager.TICK_WAITER;
            }, true))
    );

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean mouseWheelie_triggerSort() {
        if (hoveredSlot == null)
            return false;
        Player player = Minecraft.getInstance().player;
        if (player.getAbilities().instabuild
                && GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) != 0
                && (!hoveredSlot.getItem().isEmpty() == menu.getCarried().isEmpty()))
            return false;
        InventorySorter sorter = new InventorySorter(clientSort$screenHelper.get(), (AbstractContainerScreen<?>) (Object) this, hoveredSlot);
        Config.Options options = Config.get().options;
        SortMode sortMode;
        if (hasShiftDown()) {
            sortMode = options.shiftSortMode;
        } else if (hasControlDown()) {
            sortMode = options.ctrlSortMode;
        } else if (hasAltDown()) {
            sortMode = options.altSortMode;
        } else {
            sortMode = options.sortMode;
        }
        if (sortMode == null) return false;
        sorter.sort(sortMode);
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    @Unique
    private boolean clientSort$specialOperation(int button) {
        Options options = this.minecraft.options;
        if (((options.keyPickItem.matchesMouse(button)
                && this.minecraft.gameMode.hasInfiniteItems()
                && (this.hoveredSlot.hasItem()
                    || !this.draggingItem.isEmpty()
                    || !this.menu.getCarried().isEmpty())))
            || (options.keyDrop.matchesMouse(button)
                && this.hoveredSlot.hasItem())) {
            return true;
        }
        else if (options.keySwapOffhand.matchesMouse(button)) {
            return true;
        }
        else {
            for(int i = 0; i < 9; i++) {
                if (options.keyHotbarSlots[i].matchesMouse(button)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    @Unique
    private boolean clientSort$specialOperation(int keyCode, int scanCode) {
        Options options = this.minecraft.options;
        if (((options.keyPickItem.matches(keyCode, scanCode)
                && this.minecraft.gameMode.hasInfiniteItems()
                && (this.hoveredSlot.hasItem()
                    || !this.draggingItem.isEmpty()
                    || !this.menu.getCarried().isEmpty())))
            || (options.keyDrop.matches(keyCode, scanCode)
                && this.hoveredSlot.hasItem())) {
            return true;
        }
        else if (options.keySwapOffhand.matches(keyCode, scanCode)) {
            return true;
        }
        else {
            for(int i = 0; i < 9; i++) {
                if (options.keyHotbarSlots[i].matches(keyCode, scanCode)) {
                    return true;
                }
            }
        }
        return false;
    }
}
