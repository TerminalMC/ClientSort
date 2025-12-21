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

package dev.terminalmc.clientsort.client.config;

import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.TransferPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public enum Operation {
    SORT(SortPayload.TYPE, "sort"),
    STACK_FILL(StackFillPayload.TYPE, "stackFill"),
    MATCH_TRANSFER(TransferPayload.TYPE, "matchTransfer"),
    TRANSFER(TransferPayload.TYPE, "transfer");

    public final CustomPacketPayload.Type<?> type;
    public final Identifier id;
    public final String translationKey;

    Operation(CustomPacketPayload.Type<?> type, String translationKey) {
        this.type = type;
        this.id = type.id();
        this.translationKey = translationKey;
    }

    public boolean isDirectional() {
        return !this.equals(SORT);
    }
}
