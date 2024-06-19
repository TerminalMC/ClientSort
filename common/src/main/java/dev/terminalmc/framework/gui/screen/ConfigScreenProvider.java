package dev.terminalmc.framework.gui.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

import static dev.terminalmc.framework.util.Localization.localized;

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
            return new BackupScreen(parent);
        }
    }

    static class BackupScreen extends OptionsSubScreen {
        public BackupScreen(Screen parent) {
            super(parent, Minecraft.getInstance().options, localized("screen", "default"));
        }

        @Override
        public void init() {
            MultiLineTextWidget messageWidget = new MultiLineTextWidget(
                    width / 2 - 120, height / 2 - 40,
                    localized("message", "install_cloth"),
                    minecraft.font);
            messageWidget.setMaxWidth(240);
            messageWidget.setCentered(true);
            addRenderableWidget(messageWidget);

            Button openLinkButton = Button.builder(localized("message", "go_modrinth"),
                            (button) -> minecraft.setScreen(new ConfirmLinkScreen(
                                    (open) -> {
                                        if (open) Util.getPlatform().openUri("https://modrinth.com/mod/9s6osm5g");
                                        minecraft.setScreen(lastScreen);
                                    }, "https://modrinth.com/mod/9s6osm5g", true)))
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

        @Override
        protected void addOptions() {}
    }
}
