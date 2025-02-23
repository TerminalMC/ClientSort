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

package dev.terminalmc.clientsort.inventory.sort;

import dev.terminalmc.clientsort.inventory.ContainerScreenHelper;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * Additional context for executing a sort.
 *
 * @see SortOrder#sort
 */
public class SortContext {
    /**
     * The screen that is currently sorted on.
     */
    AbstractContainerScreen<?> screen;
    
    /**
     * The slots that are the target of the current sort action.
     * 
     * <p>These slots are usually in the same scope (see
     * {@link ContainerScreenHelper#getScope(Slot)}).</p>
     */
    List<Slot> relevantSlots;

    public <T> SortContext(AbstractContainerScreen<?> containerScreen, List<Slot> list) {
        this.screen = containerScreen;
        this.relevantSlots = list;
    }
}
