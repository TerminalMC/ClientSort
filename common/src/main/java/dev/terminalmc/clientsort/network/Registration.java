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

package dev.terminalmc.clientsort.network;

import dev.terminalmc.clientsort.network.handler.CollectHandler;
import dev.terminalmc.clientsort.network.handler.SortHandler;
import dev.terminalmc.clientsort.network.handler.StackFillHandler;
import dev.terminalmc.clientsort.network.handler.TransferHandler;
import dev.terminalmc.clientsort.network.payload.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Stores custom C2S payload and handler data, and S2C payload-only data, for network registration.
 */
public class Registration {

    private Registration() {
    }

    /**
     * C2S payloads with handlers.
     */
    public static final List<RegisterablePayloadC2S<?>> PAYLOADS_C2S = List.of(
            new RegisterablePayloadC2S<>(
                    CollectPayload.TYPE,
                    CollectPayload.STREAM_CODEC,
                    CollectHandler::handle
            ),
            new RegisterablePayloadC2S<>(
                    SortPayload.TYPE,
                    SortPayload.STREAM_CODEC,
                    SortHandler::handle
            ),
            new RegisterablePayloadC2S<>(
                    StackFillPayload.TYPE,
                    StackFillPayload.STREAM_CODEC,
                    StackFillHandler::handle
            ),
            new RegisterablePayloadC2S<>(
                    TransferPayload.TYPE,
                    TransferPayload.STREAM_CODEC,
                    TransferHandler::handle
            )
    );

    /**
     * S2C payloads without handlers.
     * <p>
     * <b>Note:</b> this exists because S2C payloads must be registered in
     * 'main' but the handlers cannot be accessed from there, and must instead be registered in
     * 'client'. For this purpose they can be retrieved from
     * {@link dev.terminalmc.clientsort.client.network.ClientRegistration#PAYLOADS_S2C}.
     */
    public static List<RegisterablePayloadS2C<?>> PAYLOADS_S2C = List.of(
            new RegisterablePayloadS2C<>(
                    CollectResultPayload.TYPE,
                    CollectResultPayload.STREAM_CODEC
            ),
            new RegisterablePayloadS2C<>(SortResultPayload.TYPE, SortResultPayload.STREAM_CODEC),
            new RegisterablePayloadS2C<>(
                    StackFillResultPayload.TYPE,
                    StackFillResultPayload.STREAM_CODEC
            ),
            new RegisterablePayloadS2C<>(
                    TransferResultPayload.TYPE,
                    TransferResultPayload.STREAM_CODEC
            )
    );

    /**
     * Contains registration info for a custom payload.
     */
    public abstract static class RegisterablePayload<T extends CustomPacketPayload> {

        public final CustomPacketPayload.Type<T> type;
        public final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

        public RegisterablePayload(
                CustomPacketPayload.Type<T> type,
                StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
        ) {
            this.type = type;
            this.streamCodec = streamCodec;
        }
    }

    /**
     * Contains registration info for a custom C2S payload and its handler.
     */
    public static class RegisterablePayloadC2S<T extends CustomPacketPayload>
            extends RegisterablePayload<T> {

        public final PayloadHandlerC2S<T> handler;

        public RegisterablePayloadC2S(
                CustomPacketPayload.Type<T> type,
                StreamCodec<RegistryFriendlyByteBuf, T> streamCodec,
                PayloadHandlerC2S<T> handler
        ) {
            super(type, streamCodec);
            this.handler = handler;
        }

        /**
         * Handles a custom S2C payload.
         */
        @FunctionalInterface
        public interface PayloadHandlerC2S<T extends CustomPacketPayload> {

            void accept(T payload, MinecraftServer server, ServerPlayer player);
        }
    }

    /**
     * Contains registration info for a custom S2C payload, but not its handler.
     */
    public static class RegisterablePayloadS2C<T extends CustomPacketPayload>
            extends RegisterablePayload<T> {

        public RegisterablePayloadS2C(
                CustomPacketPayload.Type<T> type,
                StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
        ) {
            super(type, streamCodec);
        }
    }
}
