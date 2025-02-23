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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A custom packet allowing inventory sorting instructions to be sent from a
 * client to the server.
 */
public class ServerboundSortPacket implements Packet<ServerGamePacketListener> {
    public static final ResourceLocation ID = new ResourceLocation(
            MainSort.MOD_ID, "sort_c2s");
    
    int syncId;
    int[] slotMapping;

    public ServerboundSortPacket(int syncId, int[] slotMapping) {
        this.syncId = syncId;
        this.slotMapping = slotMapping;
    }

    public static @Nullable ServerboundSortPacket read(FriendlyByteBuf buf) {
        int syncId = buf.readVarInt();
        int[] slotMapping = buf.readVarIntArray();

        if (slotMapping.length % 2 != 0) {
            MainSort.LOG.warn("Received sort packet with invalid data!");
            return null;
        }

        return new ServerboundSortPacket(syncId, slotMapping);
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeVarInt(syncId);
        buf.writeVarIntArray(slotMapping);
    }

    @Override
    public void handle(@NotNull ServerGamePacketListener listener) {
        
    }
}
