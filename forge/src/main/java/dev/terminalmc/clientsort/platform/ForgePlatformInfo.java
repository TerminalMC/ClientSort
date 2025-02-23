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
import dev.terminalmc.clientsort.platform.services.IPlatformInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformInfo implements IPlatformInfo {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean canSendToServer(ResourceLocation channel) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (!player.connection.isAcceptingMessages()) return false;
        return ClientSortForge.CHANNEL.isRemotePresent(player.connection.getConnection());
    }

    @Override
    public void sendToServer(ResourceLocation channel, Packet<ServerGamePacketListener> packet) {
        ClientSortForge.CHANNEL.sendToServer(packet);
    }
}
