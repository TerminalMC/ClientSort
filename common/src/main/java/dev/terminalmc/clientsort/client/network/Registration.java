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

package dev.terminalmc.clientsort.client.network;

import dev.terminalmc.clientsort.client.network.handler.CollectResultHandler;
import dev.terminalmc.clientsort.client.network.handler.SortResultHandler;
import dev.terminalmc.clientsort.client.network.handler.StackFillResultHandler;
import dev.terminalmc.clientsort.client.network.handler.TransferResultHandler;
import dev.terminalmc.clientsort.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Stores custom S2C payload and handler data for registration.
 */
public class Registration {
    /**
     * All S2C payloads with handlers.
     */
    public static List<RegisterablePayloadS2C<?>> PAYLOADS_S2C = List.of(
            new RegisterablePayloadS2C<>(
                    CollectResultPayload.TYPE,
                    CollectResultPayload.STREAM_CODEC,
                    CollectResultHandler::handle
            ),
            new RegisterablePayloadS2C<>(
                    SortResultPayload.TYPE,
                    SortResultPayload.STREAM_CODEC,
                    SortResultHandler::handle
            ),
            new RegisterablePayloadS2C<>(
                    StackFillResultPayload.TYPE,
                    StackFillResultPayload.STREAM_CODEC,
                    StackFillResultHandler::handle
            ),
            new RegisterablePayloadS2C<>(
                    TransferResultPayload.TYPE,
                    TransferResultPayload.STREAM_CODEC,
                    TransferResultHandler::handle
            )
    );

    /**
     * Contains registration info for a custom S2C payload and handler.
     * @param <T> the custom payload type.
     */
    public static class RegisterablePayloadS2C<T extends CustomPacketPayload>
            extends dev.terminalmc.clientsort.network.Registration.RegisterablePayload<T> {
        public final PayloadHandlerS2C<T> handler;

        public RegisterablePayloadS2C(
                CustomPacketPayload.Type<T> type,
                StreamCodec<RegistryFriendlyByteBuf, T> streamCodec,
                PayloadHandlerS2C<T> handler
        ) {
            super(type, streamCodec);
            this.handler = handler;
        }

        /**
         * Handles a custom S2C payload.
         * @param <T> the custom payload type.
         */
        @FunctionalInterface
        public interface PayloadHandlerS2C<T extends CustomPacketPayload> {
            void accept(T payload, Minecraft mc, LocalPlayer player);
        }
    }
}
