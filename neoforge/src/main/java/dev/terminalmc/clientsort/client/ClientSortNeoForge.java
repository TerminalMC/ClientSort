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
import dev.terminalmc.clientsort.client.network.ClientRegistration;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforgespi.locating.IModFile;

import java.util.Optional;

import static dev.terminalmc.clientsort.util.Localization.localized;

@Mod(
        value = ClientSort.MOD_ID,
        dist = Dist.CLIENT
)
@EventBusSubscriber(
        modid = ClientSort.MOD_ID,
        value = Dist.CLIENT
)
public class ClientSortNeoForge {

    public ClientSortNeoForge() {
        // Register config screen
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent)
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
                    Pack.ResourcesSupplier supplier = new PathPackResources.PathResourcesSupplier(
                            file.findResource("resourcepacks/" + packId)
                    );
                    PackLocationInfo info = new PackLocationInfo(
                            ClientSort.MOD_ID + ":" + packId,
                            localized("resourcepack", "dark-mode"),
                            PackSource.BUILT_IN,
                            Optional.empty()
                    );
                    Pack pack = Pack.readMetaAndCreate(
                            info,
                            supplier,
                            PackType.CLIENT_RESOURCES,
                            new PackSelectionConfig(false, Position.TOP, false)
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
        public static void afterClientTick(ClientTickEvent.Post event) {
            ClientSort.afterClientTick(Minecraft.getInstance());
        }
    }

    /**
     * Registers all custom S2C payloads and their handlers.
     */
    @SubscribeEvent
    static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").optional();
        ClientRegistration.PAYLOADS_S2C.forEach((rp) -> registerS2C(registrar, rp));
    }

    /**
     * Registers a custom S2C payload and its handler.
     */
    private static <T extends CustomPacketPayload> void registerS2C(
            PayloadRegistrar registrar,
            ClientRegistration.RegisterablePayloadS2C<T> rp
    ) {
        registrar.playToClient(
                rp.type,
                rp.streamCodec,
                (payload, context) -> rp.handler.accept(
                        payload,
                        Minecraft.getInstance(),
                        (LocalPlayer) context.player()
                )
        );
    }
}
