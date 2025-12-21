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

package dev.terminalmc.clientsort.network.payload;

import dev.terminalmc.clientsort.ClientSort;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A custom C2S payload used to instruct a server to collect items in a container into the smallest
 * possible number of slots.
 */
public class CollectPayload implements Packet<ServerGamePacketListener> {

    public static final ResourceLocation ID =
            new ResourceLocation(ClientSort.MOD_ID, "collect_c2s");

    int containerId;
    int[] slotIds;
    String id;

    public CollectPayload(int containerId, int[] slotIds, String id) {
        this.containerId = containerId;
        this.slotIds = slotIds;
        this.id = id;
    }

    public int containerId() {
        return containerId;
    }

    public int[] slotIds() {
        return slotIds;
    }

    public String id() {
        return id;
    }

    public static CollectPayload read(FriendlyByteBuf buf) {
        return new CollectPayload(buf.readVarInt(), buf.readVarIntArray(), buf.readUtf());
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarIntArray(slotIds);
        buf.writeUtf(id);
    }

    @Override
    public void handle(@NotNull ServerGamePacketListener listener) {

    }
}
