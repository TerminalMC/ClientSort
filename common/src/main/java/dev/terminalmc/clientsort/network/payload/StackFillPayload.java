/*
 * Copyright 2026 TerminalMC
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
 * A custom C2S payload used to instruct a server to complete as many partial item stacks in a
 * destination container as possible, using items in a source container.
 */
public class StackFillPayload implements Packet<ServerGamePacketListener> {

    public static final ResourceLocation ID =
            new ResourceLocation(ClientSort.MOD_ID, "stack_fill_c2s");

    int srcContainerId;
    int[] srcSlotIds;
    int[] dstSlotIds;
    boolean reversed;

    public StackFillPayload(
            int srcContainerId,
            int[] srcSlotIds,
            int[] dstSlotIds,
            boolean reversed
    ) {
        this.srcContainerId = srcContainerId;
        this.srcSlotIds = srcSlotIds;
        this.dstSlotIds = dstSlotIds;
        this.reversed = reversed;
    }

    public int srcContainerId() {
        return srcContainerId;
    }

    public int[] srcSlotIds() {
        return srcSlotIds;
    }

    public int[] dstSlotIds() {
        return dstSlotIds;
    }

    public boolean reversed() {
        return reversed;
    }

    public static StackFillPayload read(FriendlyByteBuf buf) {
        return new StackFillPayload(
                buf.readVarInt(),
                buf.readVarIntArray(),
                buf.readVarIntArray(),
                buf.readBoolean()
        );
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeVarInt(srcContainerId);
        buf.writeVarIntArray(srcSlotIds);
        buf.writeVarIntArray(dstSlotIds);
        buf.writeBoolean(reversed);
    }

    @Override
    public void handle(@NotNull ServerGamePacketListener listener) {

    }
}
