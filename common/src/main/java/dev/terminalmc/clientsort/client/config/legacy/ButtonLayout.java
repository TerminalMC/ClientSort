/*
 * Copyright 2026 TerminalMC
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

package dev.terminalmc.clientsort.client.config.legacy;

import dev.terminalmc.clientsort.client.config.Vec2i;
import org.jetbrains.annotations.Nullable;

public record ButtonLayout(
        String className,
        @Nullable Vec2i offset,
        @Nullable Boolean sortEnabled,
        @Nullable Boolean stackFillEnabled,
        @Nullable Boolean transferEnabled
) {

}
