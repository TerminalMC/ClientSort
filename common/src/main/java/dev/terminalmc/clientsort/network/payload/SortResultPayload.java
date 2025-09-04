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
import dev.terminalmc.clientsort.network.handler.validate.PayloadResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A custom S2C payload used to send feedback for an operation requested by a {@link SortPayload} to
 * a client.
 */
public class SortResultPayload implements Packet<ClientGamePacketListener> {

    public static final ResourceLocation ID =
            new ResourceLocation(ClientSort.MOD_ID, "sort_result_s2c");

    int result;
    String message;

    /**
     * @param result  a {@link PayloadResult} code.
     * @param message an optional message describing an error.
     */
    public SortResultPayload(int result, String message) {
        this.result = result;
        this.message = message;
    }

    public int result() {
        return result;
    }

    public String message() {
        return message;
    }

    public static SortResultPayload read(FriendlyByteBuf buf) {
        return new SortResultPayload(buf.readInt(), buf.readUtf());
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(result);
        buf.writeUtf(message);
    }

    @Override
    public void handle(@NotNull ClientGamePacketListener listener) {

    }
}
