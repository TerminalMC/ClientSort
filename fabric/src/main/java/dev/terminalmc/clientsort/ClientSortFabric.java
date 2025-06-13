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

package dev.terminalmc.clientsort;

import dev.terminalmc.clientsort.network.Registration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ClientSortFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register all custom payloads
        Registration.PAYLOADS_C2S.forEach(ClientSortFabric::registerC2S);
        Registration.PAYLOADS_S2C.forEach(ClientSortFabric::registerPayloadS2C);
    }

    /**
     * Registers a C2S payload and its handler.
     */
    private static <T extends CustomPacketPayload> void registerC2S(
            Registration.RegisterablePayloadC2S<T> rp
    ) {
        PayloadTypeRegistry.playC2S().register(
                rp.type,
                rp.streamCodec
        );
        ServerPlayNetworking.registerGlobalReceiver(
                rp.type,
                (payload, context) -> rp.handler.accept(
                        payload,
                        context.server(),
                        context.player()
                )
        );
    }

    /**
     * Registers an S2C payload, but not its handler.
     * <p>
     * <b>Note:</b> client-side payload handlers must be registered in
     * {@link dev.terminalmc.clientsort.client.ClientSortFabric}.
     */
    private static <T extends CustomPacketPayload> void registerPayloadS2C(
            Registration.RegisterablePayloadS2C<T> rp
    ) {
        PayloadTypeRegistry.playS2C().register(
                rp.type,
                rp.streamCodec
        );
    }
}
