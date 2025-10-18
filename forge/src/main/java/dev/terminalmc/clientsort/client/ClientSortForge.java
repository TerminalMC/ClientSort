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

import dev.terminalmc.clientsort.client.gui.screen.config.ConfigScreenProvider;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.forgespi.locating.IModFile;

import static dev.terminalmc.clientsort.util.Localization.localized;

@Mod.EventBusSubscriber(
        modid = ClientSort.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
@SuppressWarnings("removal")
public class ClientSortForge {

    public static void init() {
        // Register config screen
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent))
        );

        // Initialize client
        ClientSort.init();
    }

    /**
     * Registers all keybinds.
     */
    @SubscribeEvent
    static void registerKeybinds(RegisterKeyMappingsEvent event) {
        KeybindManager.KEYBINDS.forEach(event::register);
    }

    /**
     * Registers all built-in resource packs.
     */
    @SubscribeEvent
    static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType().equals(PackType.CLIENT_RESOURCES)) {
            event.addRepositorySource(((packConsumer) -> {
                String packId = "clientsort-dark-mode";
                IModFile file = ModList.get().getModFileById(ClientSort.MOD_ID).getFile();
                try {
                    final Pack pack = Pack.readMetaAndCreate(
                            ClientSort.MOD_ID + ":" + packId,
                            localized("resourcepack", "dark-mode"),
                            false,
                            id -> new PathPackResources(
                                    id,
                                    file.findResource("resourcepacks", packId),
                                    true
                            ),
                            PackType.CLIENT_RESOURCES,
                            Pack.Position.TOP,
                            PackSource.BUILT_IN
                    );
                    if (pack != null) {
                        packConsumer.accept(pack);
                    }
                } catch (NullPointerException e) {
                    e.fillInStackTrace();
                }
            }));
        }
    }

    @EventBusSubscriber(
            modid = ClientSort.MOD_ID,
            value = Dist.CLIENT
    )
    static class ClientEventHandler {

        /**
         * Registers after-tick event.
         */
        @SubscribeEvent
        public static void afterClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                ClientSort.afterClientTick(Minecraft.getInstance());
            }
        }
    }
}
