package dev.terminalmc.framework.mixin;

import dev.terminalmc.framework.Framework;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo info) {
        Framework.LOG.info("This line is printed by an example mod common mixin!");
        Framework.LOG.info("MC Version: {}", Minecraft.getInstance().getVersionType());
    }
}
