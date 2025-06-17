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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Stores custom C2S payload and handler data, and S2C payload-only data, for
 * network registration.
 */
public class Registration {
    private Registration() {}

    /**
     * C2S payloads with handlers.
     */
    public static final List<RegisterablePayloadC2S<?>> PAYLOADS_C2S = List.of(
            new RegisterablePayloadC2S<>(
                    CollectPayload.ID,
                    CollectPayload.class,
                    CollectPayload::write,
                    CollectPayload::read,
                    CollectHandler::handle
            ),
            new RegisterablePayloadC2S<>(
                    SortPayload.ID,
                    SortPayload.class,
                    SortPayload::write,
                    SortPayload::read,
                    SortHandler::handle
            ),
            new RegisterablePayloadC2S<>(
                    StackFillPayload.ID,
                    StackFillPayload.class,
                    StackFillPayload::write,
                    StackFillPayload::read,
                    StackFillHandler::handle
            ),
            new RegisterablePayloadC2S<>(
                    TransferPayload.ID,
                    TransferPayload.class,
                    TransferPayload::write,
                    TransferPayload::read,
                    TransferHandler::handle
            )
    );

    /**
     * S2C payloads without handlers.
     * <p>
     * <b>Note:</b> this exists because on Forge, all payloads must be
     * registered in 'main'. For 'client' registration, handlers must be
     * retrieved from
     * {@link dev.terminalmc.clientsort.client.network.ClientRegistration#PAYLOADS_S2C}.
     */
    public static final List<RegisterablePayloadS2C<?>> PAYLOADS_S2C = List.of(
            new RegisterablePayloadS2C<>(
                    CollectResultPayload.ID,
                    CollectResultPayload.class,
                    CollectResultPayload::write,
                    CollectResultPayload::read
            ),
            new RegisterablePayloadS2C<>(
                    SortResultPayload.ID,
                    SortResultPayload.class,
                    SortResultPayload::write,
                    SortResultPayload::read
            ),
            new RegisterablePayloadS2C<>(
                    StackFillResultPayload.ID,
                    StackFillResultPayload.class,
                    StackFillResultPayload::write,
                    StackFillResultPayload::read
            ),
            new RegisterablePayloadS2C<>(
                    TransferResultPayload.ID,
                    TransferResultPayload.class,
                    TransferResultPayload::write,
                    TransferResultPayload::read
            )
    );

    /**
     * Contains registration info for a custom payload.
     */
    public abstract static class RegisterablePayload<T extends Packet<?>> {
        public final ResourceLocation channel;
        public final Class<T> clazz;
        public final PayloadEncoder<T> encoder;
        public final PayloadDecoder<T> decoder;

        public RegisterablePayload(
                ResourceLocation id,
                Class<T> clazz,
                PayloadEncoder<T> encoder,
                PayloadDecoder<T> decoder
        ) {
            this.channel = id;
            this.clazz = clazz;
            this.encoder = encoder;
            this.decoder = decoder;
        }

        @FunctionalInterface
        public interface PayloadEncoder<T> {
            void accept(T payload, FriendlyByteBuf byteBuf);
        }

        @FunctionalInterface
        public interface PayloadDecoder<T> {
            T apply(FriendlyByteBuf byteBuf);
        }
    }

    /**
     * Contains registration info for a custom C2S payload and its handler.
     */
    public static class RegisterablePayloadC2S<T extends Packet<ServerGamePacketListener>>
            extends RegisterablePayload<T> {
        public final PayloadHandlerC2S<T> handler;

        public RegisterablePayloadC2S(
                ResourceLocation id,
                Class<T> clazz,
                PayloadEncoder<T> encoder,
                PayloadDecoder<T> decoder,
                PayloadHandlerC2S<T> handler
        ) {
            super(id, clazz, encoder, decoder);
            this.handler = handler;
        }

        /**
         * Handles a custom S2C payload.
         */
        @FunctionalInterface
        public interface PayloadHandlerC2S<T> {
            void accept(T payload, MinecraftServer server, ServerPlayer player);
        }
    }

    /**
     * Contains registration info for a custom S2C payload, but not its handler.
     */
    public static class RegisterablePayloadS2C<T extends Packet<ClientGamePacketListener>>
            extends RegisterablePayload<T> {

        public RegisterablePayloadS2C(
                ResourceLocation id,
                Class<T> clazz,
                PayloadEncoder<T> encoder,
                PayloadDecoder<T> decoder
        ) {
            super(id, clazz, encoder, decoder);
        }
    }
}
