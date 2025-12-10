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

package dev.terminalmc.clientsort.network.payload;

import dev.terminalmc.clientsort.ClientSort;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * A custom C2S payload used to instruct a server to sort slots in a container into a particular
 * order.
 *
 * @param containerId the ID of the container.
 * @param slotMapping an array of slot swap instructions, represented as pairs of the form [from
 *                    slot ID, to slot ID].
 */
public record SortPayload(int containerId, int[] slotMapping) implements CustomPacketPayload {

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, int @NotNull []> VAR_INT_ARRAY =
            new StreamCodec<>() {
                public int @NotNull [] decode(@NotNull RegistryFriendlyByteBuf byteBuf) {
                    return byteBuf.readVarIntArray();
                }

                public void encode(
                        @NotNull RegistryFriendlyByteBuf byteBuf,
                        int @NotNull [] array
                ) {
                    byteBuf.writeVarIntArray(array);
                }
            };

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull SortPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SortPayload::containerId,
                    VAR_INT_ARRAY,
                    SortPayload::slotMapping,
                    SortPayload::new
            );

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(ClientSort.MOD_ID, "sort_c2s");

    public static final CustomPacketPayload.Type<@NotNull SortPayload> TYPE = new Type<>(ID);

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }
}
