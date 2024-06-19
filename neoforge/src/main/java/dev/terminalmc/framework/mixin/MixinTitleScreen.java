package dev.terminalmc.framework.mixin;

import dev.terminalmc.framework.Framework;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "init()V", at = @At("HEAD"))
    private void init(CallbackInfo info) {
        Framework.LOG.info("This line is printed by an example mod mixin from NeoForge!");
        Framework.LOG.info("MC Version: {}", Minecraft.getInstance().getVersionType());
    }
}
