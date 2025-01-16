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

package dev.terminalmc.clientsort.inventory;

import dev.terminalmc.clientsort.network.ClickEventFactory;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class CreativeContainerScreenHelper<T extends CreativeModeInventoryScreen> extends ContainerScreenHelper<T> {
    public CreativeContainerScreenHelper(T screen, ClickEventFactory clickEventFactory) {
        super(screen, clickEventFactory);
    }

    @Override
    public int getScope(Slot slot, boolean preferSmallerScopes) {
        if (screen.isInventoryOpen()) {
            return super.getScope(slot, preferSmallerScopes);
        }
        if (slot.container instanceof Inventory) {
            if (isHotbarSlot(slot)) {
                return 0;
            }
        }
        return INVALID_SCOPE;
    }
}
