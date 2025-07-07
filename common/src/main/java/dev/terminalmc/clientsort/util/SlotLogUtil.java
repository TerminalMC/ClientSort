package dev.terminalmc.clientsort.util;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.world.inventory.Slot;

@SuppressWarnings("unused")
public class SlotLogUtil {

    public static void logSlotIndexes(Iterable<Slot> slots) {
        StringBuilder sb = new StringBuilder("[");
        for (Slot slot : slots) {
            sb.append(((ISlot) slot).clientsort$getIndexInInv());
            sb.append(":[");
            sb.append(slot.getItem().getCount());
            sb.append(" ");
            sb.append(slot.getItem().getHoverName().getString());
            sb.append("]");
            sb.append(", ");
        }
        ClientSort.LOG.warn(sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]");
    }

    public static void logSlotIds(Iterable<Slot> slots) {
        StringBuilder sb = new StringBuilder("[");
        for (Slot slot : slots) {
            sb.append(((ISlot) slot).clientsort$getIdInContainer());
            sb.append(":[");
            sb.append(slot.getItem().getCount());
            sb.append(" ");
            sb.append(slot.getItem().getHoverName().getString());
            sb.append("]");
            sb.append(", ");
        }
        ClientSort.LOG.warn(sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]");
    }

    public static void logSlotIndexArray(int[] indexes) {
        StringBuilder sb = new StringBuilder("[");
        for (int id : indexes) {
            sb.append(id);
            sb.append(", ");
        }
        ClientSort.LOG.warn(sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]");
    }

    public static void logSlotMappingArray(int[] slotMapping) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < slotMapping.length - 1; i += 2) {
            sb.append(slotMapping[i]);
            sb.append("->");
            sb.append(slotMapping[i + 1]);
            sb.append(", ");
        }
        ClientSort.LOG.warn(sb.length() == 1 ? "[]" : sb.substring(0, sb.length() - 2) + "]");
    }
}
