/*
 * Copyright 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.clientsort.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

import static dev.terminalmc.clientsort.util.mod.Localization.localized;

/**
 * <p>Wraps {@link ClothConfigScreenProvider} and provides a backup screen for
 * use when the Cloth Config mod is not loaded. This allows the dependency on
 * Cloth Config to be defined as optional.</p>
 */
public class ConfigScreenProvider {

    public static Screen getConfigScreen(Screen parent) {
        try {
            return ClothConfigScreenProvider.getConfigScreen(parent);
        }
        catch (NoClassDefFoundError ignored) {
            return new BackupScreen(parent, "install_cloth", "https://modrinth.com/mod/9s6osm5g");
        }
    }

    static class BackupScreen extends Screen {
        private final Screen parent;
        private final String modKey;
        private final String modUrl;

        public BackupScreen(Screen parent, String modKey, String modUrl) {
            super(localized("name"));
            this.parent = parent;
            this.modKey = modKey;
            this.modUrl = modUrl;
        }

        @Override
        public void init() {
            MultiLineTextWidget messageWidget = new MultiLineTextWidget(
                    width / 2 - 120, height / 2 - 40,
                    localized("message", modKey),
                    minecraft.font);
            messageWidget.setMaxWidth(240);
            messageWidget.setCentered(true);
            addRenderableWidget(messageWidget);

            Button openLinkButton = Button.builder(localized("message", "go_modrinth"),
                            (button) -> minecraft.setScreen(new ConfirmLinkScreen(
                                    (open) -> {
                                        if (open) Util.getPlatform().openUri(modUrl);
                                        minecraft.setScreen(parent);
                                    }, modUrl, true)))
                    .pos(width / 2 - 120, height / 2)
                    .size(115, 20)
                    .build();
            addRenderableWidget(openLinkButton);

            Button exitButton = Button.builder(CommonComponents.GUI_OK,
                            (button) -> onClose())
                    .pos(width / 2 + 5, height / 2)
                    .size(115, 20)
                    .build();
            addRenderableWidget(exitButton);
        }
    }
}
