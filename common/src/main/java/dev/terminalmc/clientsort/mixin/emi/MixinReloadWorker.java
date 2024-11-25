/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.mixin.emi;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.util.item.CreativeSearchOrder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
If ClientSort's CreativeSearchOrder::refreshItemSearchPositionLookup calls
CreativeModeTabs::tryRebuildTabContents while EMI is reloading, it can cause
an error.

To prevent this, the flag ClientSort::emiReloading is used. While it is true,
any calls to CreativeSearchOrder::refreshItemSearchPositionLookup will be
cancelled and ClientSort::updateBlockedByEmi will be set true to indicate that
CreativeSearchOrder::refreshItemSearchPositionLookup should be called once the
EMI reload is finished.
 */

@Pseudo
@Mixin(targets = "dev.emi.emi.runtime.EmiReloadManager$ReloadWorker", remap = false)
public class MixinReloadWorker {

    @Inject(
            method = "run",
            at = @At("HEAD")
    )
    private void onRunHead(CallbackInfo ci) {
        ClientSort.updateBlockedByEmi = false;
        ClientSort.emiReloading = true;
    }

    @Inject(
            method = "run",
            at = @At("RETURN")
    )
    private void onRunReturn(CallbackInfo ci) {
        if (ClientSort.updateBlockedByEmi) {
            CreativeSearchOrder.refreshItemSearchPositionLookup();
        }
        ClientSort.emiReloading = false;
        ClientSort.updateBlockedByEmi = false;
    }
}
