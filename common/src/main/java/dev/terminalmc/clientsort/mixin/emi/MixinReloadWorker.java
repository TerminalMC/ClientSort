/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.mixin.emi;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.main.MainSort;
import dev.terminalmc.clientsort.util.item.CreativeSearchOrder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <p>If {@link CreativeSearchOrder#refreshStackPositionMap} calls
 * {@link net.minecraft.world.item.CreativeModeTabs#tryRebuildTabContents} while
 * EMI is reloading, it can cause an error.</p>
 * 
 * <p>To prevent this, {@link ClientSort#emiReloadLock} is acquired here while
 * EMI is reloading. If {@link CreativeSearchOrder#tryRefreshStackPositionMap}
 * is called while the lock is held, the call is blocked and
 * {@link ClientSort#updateBlockedByEmi} is set to {@code true}.</p>
 * 
 * <p>When the EMI reload finishes, if {@link ClientSort#updateBlockedByEmi}
 * is {@code true}, {@link CreativeSearchOrder#tryRefreshStackPositionMap} is
 * called one time.
 * </p>
 */
@SuppressWarnings("JavadocReference")
@Pseudo
@Mixin(targets = "dev.emi.emi.runtime.EmiReloadManager$ReloadWorker", remap = false)
public class MixinReloadWorker {

    @SuppressWarnings({ "UnresolvedMixinReference", "ResultOfMethodCallIgnored" })
    @Inject(
            method = "run",
            at = @At("HEAD")
    )
    private void onRunHead(CallbackInfo ci) {
        ClientSort.updateBlockedByEmi = false;
        ClientSort.emiReloadLock.tryLock();
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
            method = "run",
            at = @At("RETURN")
    )
    private void onRunReturn(CallbackInfo ci) {
        ClientSort.emiReloadLock.unlock();
        if (ClientSort.updateBlockedByEmi) {
            MainSort.LOG.info("EMI reload finished; updating search order");
            CreativeSearchOrder.tryRefreshStackPositionMap();
            ClientSort.updateBlockedByEmi = false;
        }
    }
}
