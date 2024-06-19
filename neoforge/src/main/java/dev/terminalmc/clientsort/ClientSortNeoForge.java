package dev.terminalmc.clientsort;

import dev.terminalmc.clientsort.command.Commands;
import dev.terminalmc.clientsort.gui.screen.ConfigScreenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
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
        event.register(ClientSort.EXAMPLE_KEY);
    }

    @EventBusSubscriber(modid = ClientSort.MOD_ID, value = Dist.CLIENT)
    static class ClientEventHandler {
        // Commands
        @SubscribeEvent
        static void registerClientCommands(RegisterClientCommandsEvent event) {
            new Commands<CommandSourceStack>().register(Minecraft.getInstance(), event.getDispatcher(), event.getBuildContext());
        }

        // Tick events
        @SubscribeEvent
        public static void clientTickEvent(ClientTickEvent.Post event) {
            ClientSort.onEndTick(Minecraft.getInstance());
        }
    }
}
