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

package dev.terminalmc.clientsort.platform;

import dev.terminalmc.clientsort.ClientSortForge;
import dev.terminalmc.clientsort.platform.services.PlatformServices;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.network.PacketDistributor;

import java.nio.file.Path;

public class ForgeServices implements PlatformServices {

    @Override
    public boolean isDevEnv() {
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean canSendToPlayer(ServerPlayer player, ResourceLocation channel) {
        if (!player.connection.isAcceptingMessages())
            return false;
        return ClientSortForge.CHANNEL.isRemotePresent(player.connection.connection);
    }

    @Override
    public void sendToPlayer(
            ServerPlayer player,
            ResourceLocation channel,
            Packet<ClientGamePacketListener> packet
    ) {
        ClientSortForge.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
