/*
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

package dev.terminalmc.clientsort.compat.itemlocks;

import com.kirdow.itemlocks.client.LockManager;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import static com.kirdow.itemlocks.client.input.KeyBindings.isBypass;
import static com.kirdow.itemlocks.proxy.Components.getComponent;

public class ItemLocksCompat {
    /**
     * @param slot the slot to check.
     * @return {@code true} if the slot is valid, locked, and the bypass is not
     * active.
     */
    static boolean isLocked(Slot slot) {
        if (!(slot.container instanceof Inventory)) return false;
        int index = adjustForInventory(((ISlot) slot).clientSort$getIndexInInv());
        return getComponent(LockManager.class).isLockedSlotRaw(index) && !isBypass();
    }

    /**
     * Moves the hotbar from 0-8 to 27-35.
     */
    private static int adjustForInventory(int slot) {
        if (0 <= slot && slot <= 8) {
            return slot + 27;
        } else if (9 <= slot && slot <= 35) {
            return slot - 9;
        } else {
            return slot;
        }
    }
}
