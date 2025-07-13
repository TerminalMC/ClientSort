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

import dev.terminalmc.clientsort.client.network.ClientRegistration;
import dev.terminalmc.clientsort.command.Commands;
import dev.terminalmc.clientsort.network.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod(value = ClientSort.MOD_ID)
@Mod.EventBusSubscriber(
        modid = ClientSort.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
@SuppressWarnings("removal")
public class ClientSortForge {

    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(dev.terminalmc.clientsort.client.ClientSort.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            (v) -> NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION).test(v),
            (v) -> NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION).test(v)
    );
    public static int packetId = 0;

    public ClientSortForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::dedicatedServerSetup);
        modEventBus.addListener(this::clientSetup);
        // Initialize
        ClientSort.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        Registration.PAYLOADS_C2S.forEach(ClientSortForge::registerC2S);
    }

    private void dedicatedServerSetup(FMLDedicatedServerSetupEvent event) {
        Registration.PAYLOADS_S2C.forEach(ClientSortForge::registerS2CPayloadOnly);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        ClientRegistration.PAYLOADS_S2C.forEach(ClientSortForge::registerS2C);
        dev.terminalmc.clientsort.client.ClientSortForge.init();
    }

    @Mod.EventBusSubscriber(modid = ClientSort.MOD_ID)
    static class GameEventHandler {

        /**
         * Registers all commands.
         */
        @SubscribeEvent
        static void registerCommands(RegisterCommandsEvent event) {
            new Commands<CommandSourceStack>().register(
                    event.getDispatcher(),
                    event.getBuildContext()
            );
        }
    }

    /**
     * Registers a custom C2S payload and its handler.
     */
    @SuppressWarnings("DataFlowIssue")
    public static <T extends Packet<ServerGamePacketListener>> void registerC2S(
            Registration.RegisterablePayloadC2S<T> rp
    ) {
        CHANNEL.registerMessage(
                packetId++,
                rp.clazz,
                rp.encoder::accept,
                rp.decoder::apply,
                (packet, contextSupplier) -> {
                    rp.handler.accept(
                            packet,
                            contextSupplier.get().getSender().getServer(),
                            contextSupplier.get().getSender()
                    );
                    contextSupplier.get().setPacketHandled(true);
                },

                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    /**
     * Registers a custom S2C payload but not its handler.
     */
    public static <T extends Packet<ClientGamePacketListener>> void registerS2CPayloadOnly(
            Registration.RegisterablePayloadS2C<T> rp
    ) {
        CHANNEL.registerMessage(
                packetId++,
                rp.clazz,
                rp.encoder::accept,
                rp.decoder::apply,
                (packet, contextSupplier) -> contextSupplier.get().setPacketHandled(true),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    /**
     * Registers a custom S2C payload and its handler.
     */
    public static <T extends Packet<ClientGamePacketListener>> void registerS2C(
            ClientRegistration.RegisterablePayloadS2C<T> rp
    ) {
        CHANNEL.registerMessage(
                packetId++,
                rp.clazz,
                rp.encoder::accept,
                rp.decoder::apply,
                (packet, contextSupplier) -> {
                    rp.handler.accept(
                            packet,
                            Minecraft.getInstance(),
                            Minecraft.getInstance().player
                    );
                    contextSupplier.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
}
