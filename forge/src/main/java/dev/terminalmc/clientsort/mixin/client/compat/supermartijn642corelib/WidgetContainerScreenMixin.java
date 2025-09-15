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

package dev.terminalmc.clientsort.mixin.client.compat.supermartijn642corelib;

import dev.terminalmc.clientsort.client.gui.widget.TriggerButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * For reasons unknown, SuperMartijn642's {@link com.supermartijn642.core.gui.WidgetContainerScreen}
 * implementation does not call the super render method, so the screen's children are never
 * rendered.
 * <p>
 * This mixin works around that by injecting into the render method and manually rendering the
 * ClientSort buttons.
 */
@Pseudo
@Mixin(targets = "com.supermartijn642.core.gui.WidgetContainerScreen")
@SuppressWarnings("JavadocReference")
public class WidgetContainerScreenMixin extends Screen {

    protected WidgetContainerScreenMixin(Component title) {
        super(title);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
            method = "m_88315_",
            at = @At("RETURN")
    )
    private void afterRender(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        this.children().forEach((child) -> {
            if (child instanceof TriggerButton button) {
                button.render(graphics, mouseX, mouseY, partialTick);
            }
        });
    }
}
