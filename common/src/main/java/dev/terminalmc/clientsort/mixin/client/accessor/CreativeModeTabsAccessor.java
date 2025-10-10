package dev.terminalmc.clientsort.mixin.client.accessor;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeTabs.class)
public interface CreativeModeTabsAccessor {

    @Accessor("CACHED_PARAMETERS")
    static void setCachedParameters(CreativeModeTab.ItemDisplayParameters newParams) {
        throw new AssertionError();
    }

}
