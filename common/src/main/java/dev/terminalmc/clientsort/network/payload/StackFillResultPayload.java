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
 * A custom payload allowing feedback to be sent from server to client.
 * @param success whether the operation was successful.
 * @param message an optional message describing an error.
 */
public record StackFillResultPayload(boolean success, String message) implements CustomPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, StackFillResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, StackFillResultPayload::success,
                    ByteBufCodecs.STRING_UTF8, StackFillResultPayload::message,
                    StackFillResultPayload::new
            );

    public static final ResourceLocation TYPE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "stack_fill_result_s2c");

    public static final Type<StackFillResultPayload> TYPE =
            new Type<>(TYPE_LOCATION);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
