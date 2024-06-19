package dev.terminalmc.clientsort;

import dev.terminalmc.clientsort.util.CreativeSearchOrder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ClientSortFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Keybindings
        KeyBindingHelper.registerKeyBinding(ClientSort.SORT_KEY);

        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(ClientSort::onEndTick);

        // Game join events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                CreativeSearchOrder.refreshItemSearchPositionLookup());

        // Main initialization
        ClientSort.init();
    }
}
