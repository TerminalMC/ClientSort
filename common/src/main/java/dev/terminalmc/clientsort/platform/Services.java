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

package dev.terminalmc.clientsort.platform;

import dev.terminalmc.clientsort.main.MainSort;
import dev.terminalmc.clientsort.platform.services.IPlatformInfo;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatformInfo PLATFORM = load(IPlatformInfo.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "Failed to load service for " + clazz.getName()));
        MainSort.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
