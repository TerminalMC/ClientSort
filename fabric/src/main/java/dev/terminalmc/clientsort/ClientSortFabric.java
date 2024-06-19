package dev.terminalmc.clientsort;

import dev.terminalmc.clientsort.command.Commands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;

public class ClientSortFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Keybindings
        KeyBindingHelper.registerKeyBinding(ClientSort.EXAMPLE_KEY);

        // Commands
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, buildContext) ->
                new Commands<FabricClientCommandSource>().register(Minecraft.getInstance(), dispatcher, buildContext)));

        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(ClientSort::onEndTick);

        // Main initialization
        ClientSort.init();
    }
}
