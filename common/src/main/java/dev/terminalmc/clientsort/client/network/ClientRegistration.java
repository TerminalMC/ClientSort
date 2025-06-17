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
import dev.terminalmc.clientsort.network.Registration;
import dev.terminalmc.clientsort.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Stores custom S2C payload and handler data for network registration.
 */
public class ClientRegistration {
    private ClientRegistration() {}

    /**
     * S2C payloads with handlers.
     */
    public static List<RegisterablePayloadS2C<?>> PAYLOADS_S2C = List.of(
            new RegisterablePayloadS2C<>(
                    CollectResultPayload.ID,
                    CollectResultPayload.class,
                    CollectResultPayload::write,
                    CollectResultPayload::read,
                    CollectResultHandler::handle
            ),
            new RegisterablePayloadS2C<>(
                    SortResultPayload.ID,
                    SortResultPayload.class,
                    SortResultPayload::write,
                    SortResultPayload::read,
                    SortResultHandler::handle
            ),
            new RegisterablePayloadS2C<>(
                    StackFillResultPayload.ID,
                    StackFillResultPayload.class,
                    StackFillResultPayload::write,
                    StackFillResultPayload::read,
                    StackFillResultHandler::handle
            ),
            new RegisterablePayloadS2C<>(
                    TransferResultPayload.ID,
                    TransferResultPayload.class,
                    TransferResultPayload::write,
                    TransferResultPayload::read,
                    TransferResultHandler::handle
            )
    );

    /**
     * Contains registration info for a custom S2C payload and its handler.
     */
    public static class RegisterablePayloadS2C<T extends Packet<ClientGamePacketListener>>
            extends Registration.RegisterablePayload<T> {
        public final PayloadHandlerS2C<T> handler;

        public RegisterablePayloadS2C(
                ResourceLocation id,
                Class<T> clazz,
                PayloadEncoder<T> encoder,
                PayloadDecoder<T> decoder,
                PayloadHandlerS2C<T> handler
        ) {
            super(id, clazz, encoder, decoder);
            this.handler = handler;
        }

        /**
         * Handles a custom S2C payload.
         */
        @FunctionalInterface
        public interface PayloadHandlerS2C<T> {
            void accept(T payload, Minecraft mc, LocalPlayer player);
        }
    }
}
