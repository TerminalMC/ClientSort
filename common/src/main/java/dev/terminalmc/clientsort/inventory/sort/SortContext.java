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

package dev.terminalmc.clientsort.inventory.sort;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Additional context for executing a sort.
 *
 * @see SortMode#sort(int[], ItemStack[], SortContext)
 */
public class SortContext {
	/**
	 * The screen that is currently sorted on.
	 */
	AbstractContainerScreen<?> screen;
	/**
	 * The slots that are the target of the current sort action.
	 * These slots are usually in the same scope (see {@link dev.terminalmc.clientsort.inventory.ContainerScreenHelper.getScope(Slot)}).
	 */
	List<Slot> relevantSlots;

	public <T> SortContext(AbstractContainerScreen<?> containerScreen, List<Slot> list) {
		this.screen = containerScreen;
		this.relevantSlots = list;
	}
}
