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

import dev.terminalmc.clientsort.command.ModCommands;
import dev.terminalmc.clientsort.mixin.accessor.ServerCommonPacketListenerImplAccessor;
import dev.terminalmc.clientsort.network.Registration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Objects;

@Mod(ClientSort.MOD_ID)
@EventBusSubscriber(modid = ClientSort.MOD_ID)
public class ClientSortNeoForge {

    public ClientSortNeoForge() {
        // Initialize
        ClientSort.init();
    }

    /**
     * Registers all custom C2S payloads and their handlers.
     */
    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").optional();
        Registration.PAYLOADS_C2S.forEach((rp) -> registerC2S(registrar, rp));
    }

    @EventBusSubscriber(modid = ClientSort.MOD_ID)
    static class GameEventHandler {

        /**
         * Registers all commands.
         */
        @SubscribeEvent
        static void registerCommands(RegisterCommandsEvent event) {
            new ModCommands<CommandSourceStack>().register(
                    event.getDispatcher(),
                    event.getBuildContext()
            );
        }
    }

    @EventBusSubscriber(
            modid = ClientSort.MOD_ID,
            value = Dist.DEDICATED_SERVER
    )
    static class DedicatedServerEventHandler {

        /**
         * Registers all custom S2C payloads, but not their handlers.
         */
        @SubscribeEvent
        static void register(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1").optional();
            Registration.PAYLOADS_S2C.forEach((rp) -> registerPayloadS2C(registrar, rp));
        }
    }

    /**
     * Registers a custom C2S payload and its handler.
     */
    private static <T extends CustomPacketPayload> void registerC2S(
            PayloadRegistrar registrar,
            Registration.RegisterablePayloadC2S<T> rp
    ) {
        registrar.playToServer(
                rp.type,
                rp.streamCodec,
                (payload, context) -> rp.handler.accept(
                        payload,
                        ((ServerCommonPacketListenerImplAccessor) Objects.requireNonNull(
                                context.connection().getPacketListener())
                        ).clientsort$getServer(),
                        (ServerPlayer) context.player()
                )
        );
    }

    /**
     * Registers a custom S2C payload, but not its handler.
     * <p>
     * <b>Note:</b> client-side payload handlers must be registered in
     * {@link dev.terminalmc.clientsort.client.ClientSortNeoForge}.
     */
    private static <T extends CustomPacketPayload> void registerPayloadS2C(
            PayloadRegistrar registrar,
            Registration.RegisterablePayloadS2C<T> rp
    ) {
        registrar.playToClient(
                rp.type,
                rp.streamCodec,
                (payload, context) -> {
                }
        );
    }
}
