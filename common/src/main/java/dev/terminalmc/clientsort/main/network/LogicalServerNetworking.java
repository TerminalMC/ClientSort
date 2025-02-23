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

package dev.terminalmc.clientsort.main.network;

import dev.terminalmc.clientsort.main.MainSort;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Handles sorting via a {@link SortPayload} on the logical server side.
 */
public class LogicalServerNetworking {
	private LogicalServerNetworking() {}

    /**
     * Handles a {@link SortPayload} sent by a client.
     */
    @SuppressWarnings("ConstantConditions")
	public static void onSortPayload(SortPayload payload, MinecraftServer server,
                                     ServerPlayer player) {
		if (player.containerMenu == null) {
			MainSort.LOG.warn("Player {} tried to sort inventory without having an " +
                    "open container!", player);
			return;
		}

		if (payload.syncId() == player.inventoryMenu.containerId) {
            // Sort inventory
			server.execute(() -> sort(player, player.inventoryMenu, payload.slotMapping()));
		} else if (payload.syncId() == player.containerMenu.containerId) {
            // Sort container
			server.execute(() -> sort(player, player.containerMenu, payload.slotMapping()));
		}
	}

    /**
     * Sorts the specified inventory according to the provided slot mapping.
     */
	private static void sort(Player player, AbstractContainerMenu screenHandler,
                             int[] slotMapping) {
		if (!validMapping(player, screenHandler, slotMapping)) {
			MainSort.LOG.warn("Sort payload from player {} contains invalid data, ignoring!",
                    player);
			return;
		}

		List<ItemStack> stacks = screenHandler.slots.stream().map(Slot::getItem).toList();

		for (int i = 0; i < slotMapping.length; i += 2) {
			int originSlotId = slotMapping[i];
			int destSlotId = slotMapping[i + 1];

			screenHandler.slots.get(destSlotId).setByPlayer(stacks.get(originSlotId));
		}
	}

    /**
     * @return {@code true} if the specified slot mapping is valid.
     */
	private static boolean validMapping(Player player, AbstractContainerMenu screenHandler,
                                        int[] slotMapping) {
		if (slotMapping.length < 4) {
			MainSort.LOG.warn("Sort payload contains too few slots!");
			return false;
		}

		IntSet requestedSlots = new IntAVLTreeSet();
		Container targetInv;

        if (!validSlotId(screenHandler, slotMapping[0])) {
            return false;
        }
		Slot firstSlot = screenHandler.slots.get(slotMapping[0]);
		targetInv = firstSlot.container;

        // Check each slot mapping
		for (int i = 0; i < slotMapping.length; i += 2) {
			int originSlotId = slotMapping[i];
			int destSlotId = slotMapping[i + 1];
            
			if (!validSlot(screenHandler, originSlotId, targetInv)) {
				return false;
			}
            
			if (!requestedSlots.add(originSlotId)) {
				MainSort.LOG.warn("Sort payload contains duplicate origin slot {}!",
                        originSlotId);
				return false;
			}

			if (!validSlot(screenHandler, destSlotId, targetInv)) {
				return false;
			}

			if (originSlotId == destSlotId) {
				continue;
			}

			Slot originSlot = screenHandler.getSlot(originSlotId);
			if (!originSlot.mayPickup(player)) {
				MainSort.LOG.warn("Player {} tried to sort slot {}, but that slot " +
                        "doesn't allow taking items!", player, originSlotId);
				return false;
			}
            
			Slot destSlot = screenHandler.getSlot(destSlotId);
			if (!destSlot.mayPlace(originSlot.getItem())) {
				MainSort.LOG.warn("Player {} tried to sort slot {}, but that slot " +
                        "doesn't allow inserting the origin stack!", player, destSlotId);
				return false;
			}
		}

		for (int i = 1; i < slotMapping.length; i += 2) {
			int destSlotId = slotMapping[i];
			if (!requestedSlots.remove(destSlotId)) {
				MainSort.LOG.warn("Sort payload contains duplicate destination slot or " +
                        "slot without origin: {}!", i);
				return false;
			}
		}
		if (!requestedSlots.isEmpty()) {
			MainSort.LOG.error("Invalid state during checking sort payload, please report " +
                            "this to the {} developer. Requested slots: {}",
                    MainSort.MOD_NAME, requestedSlots);
			return false;
		}
		return true;
	}

    /**
     * @return {@code true} if the specified slot ID is a valid index.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean validSlotId(AbstractContainerMenu screenHandler, int slotId) {
        if (slotId < 0 || slotId >= screenHandler.slots.size()) {
            MainSort.LOG.warn("Sort payload contains invalid slot id {} out of bounds " +
                            "for length {}!", slotId, screenHandler.slots.size());
            return false;
        }
        
        return true;
    }

    /**
     * @return {@code true} if the specified slot ID refers to a slot in the
     * specified {@link Container}.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean validSlot(AbstractContainerMenu screenHandler, int slotId, Container targetInv) {
        if (!validSlotId(screenHandler, slotId)) {
            return false;
        }
        
        Slot slot = screenHandler.getSlot(slotId);

		if (targetInv != slot.container) {
			MainSort.LOG.warn("Sort payload contains slots from different inventories, " +
                    "first: {}, now: {}!", targetInv, slot.container);
			return false;
		}
        
		return true;
	}
}
