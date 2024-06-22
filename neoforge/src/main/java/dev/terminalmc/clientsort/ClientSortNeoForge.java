/*
 * Copyright 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.clientsort;

import dev.terminalmc.clientsort.screen.ConfigScreenProvider;
import dev.terminalmc.clientsort.util.CreativeSearchOrder;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ClientSort.MOD_ID)
@EventBusSubscriber(modid = ClientSort.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSortNeoForge {
    public ClientSortNeoForge() {
        // Config screen
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent));

        // Main initialization
        ClientSort.init();
    }

    // Keybindings
    @SubscribeEvent
    static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
        event.register(ClientSort.SORT_KEY);
    }

    @EventBusSubscriber(modid = ClientSort.MOD_ID, value = Dist.CLIENT)
    static class ClientEventHandler {
        // Game join events
        @SubscribeEvent
        public static void loginEvent(ClientPlayerNetworkEvent.LoggingIn event) {
            CreativeSearchOrder.refreshItemSearchPositionLookup();
        }

        // Tick events
        @SubscribeEvent
        public static void clientTickEvent(ClientTickEvent.Post event) {
            ClientSort.onEndTick(Minecraft.getInstance());
        }
    }
}
