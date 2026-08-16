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

package dev.terminalmc.clientsort.client.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ClientServices {

    private static final Logger LOGGER = LoggerFactory.getLogger("ClientSort (Client Service)");

    private ClientServices() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz, clazz.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "Failed to load service for " + clazz.getName()));
        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    public static <T> T loadOr(Class<T> clazz, Supplier<T> supplier) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElse(supplier.get());
        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
