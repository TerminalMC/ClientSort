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

package dev.terminalmc.clientsort.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static dev.terminalmc.clientsort.util.Localization.localized;
import static net.minecraft.commands.Commands.literal;

@SuppressWarnings("unchecked")
public class Commands {

    private Commands() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static <S> void register(CommandDispatcher<S> dispatcher, CommandBuildContext buildCtx) {
        //noinspection unchecked
        dispatcher.register((LiteralArgumentBuilder<S>) literal(ClientSort.MOD_ID)
                .requires((sourceStack) -> sourceStack.hasPermission(2))
                .then(literal("reload")
                        .executes(ctx -> {
                            MutableComponent msg = Component.empty()
                                    .append(Component.literal("[")
                                            .withStyle(ChatFormatting.DARK_GRAY))
                                    .append(Component.literal("Client")
                                            .withStyle(ChatFormatting.AQUA))
                                    .append(Component.literal("Sort")
                                            .withStyle(ChatFormatting.DARK_AQUA))
                                    .append(Component.literal("] ")
                                            .withStyle(ChatFormatting.DARK_GRAY))
                                    .withStyle(ChatFormatting.GRAY);

                            ServerConfig.reloadAndSave();
                            msg.append(localized(
                                    "message",
                                    "configReloaded",
                                    Component.literal(ServerConfig.FILE_NAME)
                                            .withStyle(ChatFormatting.GOLD)
                            ));
                            ctx.getSource().sendSystemMessage(msg);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }
}
