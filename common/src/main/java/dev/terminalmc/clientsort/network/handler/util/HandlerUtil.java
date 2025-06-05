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

package dev.terminalmc.clientsort.network.handler.util;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.exception.ClientSortException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

public class HandlerUtil {
    public static @NotNull AbstractContainerMenu getMenu(int id, ServerPlayer player)
            throws ClientSortException {
        if (!player.containerMenu.stillValid(player)) {
            ClientSort.LOG.warn("Player {} interacted with invalid menu {}", player, player.containerMenu);
        }

        AbstractContainerMenu menu;
        if (id == player.inventoryMenu.containerId) {
            menu = player.inventoryMenu;
        } else if (id == player.containerMenu.containerId) {
            menu = player.containerMenu;
        } else {
            throw new ClientSortException(String.format(
                    "Container ID '%s' does not match player inventory or container!",
                    id));
        }
        return menu;
    }

    @SuppressWarnings("unused")
    private static void logScreenHandlerSlots(AbstractContainerMenu screenHandler) {
        // Log inventory array
        StringBuilder arr = new StringBuilder("[");
        for (Slot slot : screenHandler.slots) {
            arr.append(slot.index);
            arr.append(":");
            arr.append(slot.getItem().getDisplayName().getString());
            arr.append(" x");
            arr.append(slot.getItem().getCount());
            arr.append(", ");
        }
        ClientSort.LOG.warn(arr.length() == 1 ? "[]" : arr.substring(0, arr.length() - 2) + "]");
    }

    @SuppressWarnings("unused")
    private static void logSlotMapping(int[] slotMapping) {
        StringBuilder arr = new StringBuilder("[");
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            arr.append(slotMapping[i]);
            arr.append("->");
            arr.append(slotMapping[i+1]);
            arr.append(", ");
        }
        ClientSort.LOG.warn(arr.length() == 1 ? "[]" : arr.substring(0, arr.length() - 2) + "]");
    }

    @SuppressWarnings("unused")
    private static void logSlotArray(int[] slotIds) {
        StringBuilder arr = new StringBuilder("[");
        for (int id : slotIds) {
            arr.append(id);
            arr.append(", ");
        }
        ClientSort.LOG.warn(arr.length() == 1 ? "[]" : arr.substring(0, arr.length() - 2) + "]");
    }
}
