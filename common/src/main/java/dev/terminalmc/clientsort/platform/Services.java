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

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.platform.services.IPlatformServices;

import java.util.ServiceLoader;

public class Services {

    public static final IPlatformServices PLATFORM = load(IPlatformServices.class);

    public static <T> T load(Class<T> service) {
        final T loadedService = ServiceLoader.load(service, service.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "Failed to load service for " + service.getName()));
        ClientSort.LOG.debug("Loaded {} for service {}", loadedService, service);
        return loadedService;
    }
}
