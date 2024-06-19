package dev.terminalmc.framework.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.terminalmc.framework.Framework;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@SuppressWarnings("unchecked")
public class Commands<S> extends CommandDispatcher<S> {
    public void register(Minecraft mc, CommandDispatcher<S> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register((LiteralArgumentBuilder<S>)literal(Framework.MOD_ID)
                .then(literal("quote")
                        .then(argument("word", StringArgumentType.word())
                                .suggests(((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        List.of("Hello", "World"), builder)))
                                .executes(ctx -> {
                                    String word = StringArgumentType.getString(ctx, "word");

                                    MutableComponent msg = Framework.PREFIX.copy();
                                    msg.append(word);

                                    mc.player.sendSystemMessage(msg);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("name")
                        .then(literal("item")
                                .then(argument("item", ItemArgument.item(buildContext))
                                        .executes(ctx -> {
                                            Item item = ItemArgument.getItem(ctx, "item").getItem();

                                            MutableComponent msg = Framework.PREFIX.copy();
                                            msg.append(item.getDescription());

                                            mc.player.sendSystemMessage(msg);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(literal("color")
                                .then(argument("color", ColorArgument.color())
                                        .executes(ctx -> {
                                            ChatFormatting color = ColorArgument.getColor(ctx, "color");

                                            MutableComponent msg = Framework.PREFIX.copy();
                                            msg.append(Component.literal(color.getName()).withStyle(color));

                                            mc.player.sendSystemMessage(msg);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                )
        );
    }
}
