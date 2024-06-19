/*
 * Copyright 2020-2022 Siphalor
 * Copyright 2024 NotRyken
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package dev.terminalmc.clientsort.mixin;

import com.google.common.base.Suppliers;
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.inventory.ContainerScreenHelper;
import dev.terminalmc.clientsort.inventory.sort.InventorySorter;
import dev.terminalmc.clientsort.inventory.sort.SortMode;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.inject.IContainerScreen;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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
		} else {
			sortMode = options.sortMode;
		}
		if (sortMode == null) return false;
		sorter.sort(sortMode);
		return true;
	}
}
