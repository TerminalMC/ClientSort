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

package dev.terminalmc.clientsort.util;

import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

@SuppressWarnings("unused")
public class SlotLogUtil {

    public static String listSlotIndexes(Iterable<Slot> slots) {
        StringBuilder sb = new StringBuilder("[");
        for (Slot slot : slots) {
            sb.append(((ISlot) slot).clientsort$getIndexInInv());
            sb.append(":[");
            sb.append(slot.getItem().getCount());
            sb.append(" ");
            sb.append(slot.getItem().getHoverName().getString());
            sb.append("], ");
        }
        return sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]";
    }

    public static String listSlotIds(Iterable<Slot> slots) {
        StringBuilder sb = new StringBuilder("[");
        for (Slot slot : slots) {
            sb.append(((ISlot) slot).clientsort$getIdInContainer());
            sb.append(":[");
            sb.append(slot.getItem().getCount());
            sb.append(" ");
            sb.append(slot.getItem().getHoverName().getString());
            sb.append("], ");
        }
        return sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]";
    }

    public static String listSlotIndexArray(int[] indexes) {
        StringBuilder sb = new StringBuilder("[");
        for (int id : indexes) {
            sb.append(id);
            sb.append(", ");
        }
        return sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]";
    }

    public static String listSlotMappingArray(int[] slotMapping) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            sb.append(slotMapping[i]);
            sb.append("->");
            sb.append(slotMapping[i + 1]);
            sb.append(", ");
        }
        return sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]";
    }

    private static String listContainerMenuSlots(AbstractContainerMenu menu) {
        StringBuilder sb = new StringBuilder("[");
        for (Slot slot : menu.slots) {
            sb.append(slot.index);
            sb.append(":[");
            sb.append(slot.getItem().getCount());
            sb.append(" ");
            sb.append(slot.getItem().getDisplayName().getString());
            sb.append("], ");
        }
        return sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]";
    }
}
