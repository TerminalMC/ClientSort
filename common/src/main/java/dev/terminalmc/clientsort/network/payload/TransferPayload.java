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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A custom C2S payload used to instruct a server to transfer as many items as possible from a
 * source container to a destination container.
 *
 * @param srcContainerId the ID of the source container.
 * @param srcSlotIds     a sub-array of slots to take items from.
 * @param dstSlotIds     a sub-array of slots to place items in.
 */
public record TransferPayload(int srcContainerId, int[] srcSlotIds, int[] dstSlotIds)
        implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, int[]> VAR_INT_ARRAY =
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

    public static final StreamCodec<RegistryFriendlyByteBuf, TransferPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    TransferPayload::srcContainerId,
                    VAR_INT_ARRAY,
                    TransferPayload::srcSlotIds,
                    VAR_INT_ARRAY,
                    TransferPayload::dstSlotIds,
                    TransferPayload::new
            );

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "transfer_c2s");

    public static final Type<TransferPayload> TYPE = new Type<>(ID);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
