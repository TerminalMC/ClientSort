/*
 * Copyright 2024 TerminalMC
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

package dev.terminalmc.clientsort.gui.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

import static dev.terminalmc.clientsort.util.mod.Localization.localized;

/**
 * Wraps the config screen implementation and provides a backup screen for
 * use when the config lib mod is not loaded. This allows the dependency to be
 * defined as optional.
 */
public class ConfigScreenProvider {

    public static Screen getConfigScreen(Screen parent) {
        try {
            return ClothScreenProvider.getConfigScreen(parent);
        } catch (NoClassDefFoundError ignored) {
            return new BackupScreen(parent, "installCloth", "https://modrinth.com/mod/9s6osm5g");
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

            Button openLinkButton = Button.builder(localized("message", "viewModrinth"),
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
