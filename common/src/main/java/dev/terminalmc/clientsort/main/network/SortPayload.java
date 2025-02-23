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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A custom payload allowing inventory sorting instructions to be sent from a
 * client to the server.
 * @param syncId the ID of the container to sort.
 * @param slotMapping the array of slot swap operations.
 */
public record SortPayload(int syncId, int[] slotMapping) implements CustomPacketPayload {
    
    public static final StreamCodec<RegistryFriendlyByteBuf, int[]> VAR_INT_ARRAY =
            new StreamCodec<>() {
        public int @NotNull [] decode(@NotNull RegistryFriendlyByteBuf byteBuf) {
            return byteBuf.readVarIntArray();
        }

        public void encode(@NotNull RegistryFriendlyByteBuf byteBuf, int @NotNull [] array) {
            byteBuf.writeVarIntArray(array);
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SortPayload::syncId,
                    VAR_INT_ARRAY,
                    SortPayload::slotMapping,
                    SortPayload::new
            );
    
    public static final ResourceLocation TYPE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(MainSort.MOD_ID, "reorder_inventory_c2s");
    
    public static final CustomPacketPayload.Type<SortPayload> TYPE =
            new Type<>(TYPE_LOCATION);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
