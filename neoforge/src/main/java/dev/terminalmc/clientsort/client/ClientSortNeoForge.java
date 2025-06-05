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

package dev.terminalmc.clientsort.client;

import dev.terminalmc.clientsort.client.gui.screen.ConfigScreenProvider;
import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.network.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(value = ClientSort.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = ClientSort.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSortNeoForge {
    public ClientSortNeoForge() {
        // Config screen
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent));

        // Client initialization
        dev.terminalmc.clientsort.client.ClientSort.init();
    }

    // Keybindings
    @SubscribeEvent
    static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        dev.terminalmc.clientsort.client.ClientSort.KEYBINDS.forEach(event::register);
    }

    @EventBusSubscriber(modid = ClientSort.MOD_ID, value = Dist.CLIENT)
    static class ClientEventHandler {
        // Tick events
        @SubscribeEvent
        public static void clientTickEvent(ClientTickEvent.Post event) {
            dev.terminalmc.clientsort.client.ClientSort.onEndTick(Minecraft.getInstance());
        }
    }

    // Custom payloads
    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").optional();
        Registration.PAYLOADS_S2C.forEach((rp) -> registerS2C(registrar, rp));
    }

    /**
     * S2C; register payload and handler here.
     */
    private static <T extends CustomPacketPayload> void registerS2C(
            PayloadRegistrar registrar,
            Registration.RegisterablePayloadS2C<T> rp
    ) {
        registrar.playToClient(
                rp.type,
                rp.streamCodec,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> rp.handler.accept(
                                payload,
                                Minecraft.getInstance(),
                                (LocalPlayer)context.player()
                        ),
                        (payload, context) -> rp.handler.accept(
                                payload,
                                Minecraft.getInstance(),
                                (LocalPlayer)context.player()
                        )
                )
        );
    }
}
