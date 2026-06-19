/*
 * Copyright 2026 TerminalMC
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

package dev.terminalmc.clientsort.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.gui.screen.config.ConfigScreenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Commands {

    private Commands() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static <S> void register(CommandDispatcher<S> dispatcher, CommandBuildContext buildCtx) {
        Minecraft mc = Minecraft.getInstance();
        //noinspection unchecked
        dispatcher.register((LiteralArgumentBuilder<S>) literal(ClientSort.MOD_ID)
                .executes((ctx) -> {
                    mc.schedule(() -> mc.gui.setScreen(ConfigScreenProvider.getConfigScreen(null)));
                    return Command.SINGLE_SUCCESS;
                })
                // fall-through to server commands
                .then(argument("_", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String cmd = StringArgumentType.getString(ctx, "_");
                            // jump directly to packet-send to bypass loader command interception
                            mc.player.connection.send(
                                    new ServerboundChatCommandPacket(ClientSort.MOD_ID + " " + cmd)
                            );
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }
}
