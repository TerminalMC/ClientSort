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

import dev.terminalmc.clientsort.gui.screen.ConfigScreenProvider;
import dev.terminalmc.clientsort.main.MainSort;
import dev.terminalmc.clientsort.main.network.LogicalServerNetworking;
import dev.terminalmc.clientsort.main.network.ServerboundSortPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod(MainSort.MOD_ID)
@Mod.EventBusSubscriber(modid = MainSort.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSortForge {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MainSort.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            (v) -> NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION).test(v),
            (v) -> NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION).test(v)
    );
    
    public ClientSortForge() {
        //noinspection removal
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::commonSetup);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        // Config screen
        //noinspection removal
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent)));

        // Client initialization
        ClientSort.init();
    }
    
    public void commonSetup(FMLCommonSetupEvent event) {
        // Server networking
        //noinspection DataFlowIssue
        CHANNEL.registerMessage(
                0,
                ServerboundSortPacket.class,
                ServerboundSortPacket::write,
                ServerboundSortPacket::read,
                (packet, contextSupplier) ->
                        LogicalServerNetworking.onSortPayload(
                                packet,
                                contextSupplier.get().getSender().getServer(),
                                contextSupplier.get().getSender()
                        ),
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    // Keybindings
    @SubscribeEvent
    static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(ClientSort.SORT_KEY);
    }
}
