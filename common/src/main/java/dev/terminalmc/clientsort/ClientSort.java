package dev.terminalmc.clientsort;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.gui.screen.ConfigScreenProvider;
import dev.terminalmc.clientsort.util.ModLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static dev.terminalmc.clientsort.util.Localization.translationKey;

public class ClientSort {
    public static final String MOD_ID = "clientsort";
    public static final String MOD_NAME = "ClientSort";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(MOD_NAME).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.GRAY);
    public static final KeyMapping EXAMPLE_KEY = new KeyMapping(
            translationKey("key", "example"), InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), translationKey("key_group"));

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft mc) {
        while (EXAMPLE_KEY.consumeClick()) {
            mc.setScreen(ConfigScreenProvider.getConfigScreen(mc.screen));
        }
    }

    public static void onConfigSaved(Config config) {
        // If you are maintaining caches based on config values, update them here.
    }
}
