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

package dev.terminalmc.clientsort.client.util;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.client.config.Config.options;

public class PolicyManager {

    private PolicyManager() {
    }

    private static final Set<Class<?>> policyClasses = new LinkedHashSet<>();

    /**
     * Reloads the cache of policy configuration classes.
     */
    public static void reloadPolicyClasses(Set<String> classNames) {
        policyClasses.clear();
        for (String className : classNames) {
            try {
                policyClasses.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                if (debug()) {
                    ClientSort.LOG.warn(
                            "Unable to load policy class '{}': Class not found.",
                            className
                    );
                }
            }
        }
    }

    /**
     * @return the lowest-degree matching policy for the specified class, if any exists.
     */
    public static @Nullable ClassPolicy getPolicy(Class<?> cls) {
        // Check for a perfect match
        ClassPolicy policy = options().classPolicies.get(cls.getName());
        if (policy != null)
            return policy;

        // No perfect match; find all higher-degree matching classes
        Set<Class<?>> matches = policyClasses.stream()
                .filter(c -> c.isAssignableFrom(cls))
                .collect(Collectors.toSet());

        // Double-iterate to find the lowest-degree match
        for (Class<?> c1 : matches) {
            boolean hasSubclass = false;
            // If any c2 is a subclass of c1, c1 is not lowest
            for (Class<?> c2 : matches) {
                if (!c1.equals(c2) && c1.isAssignableFrom(c2)) {
                    hasSubclass = true;
                    break;
                }
            }
            if (!hasSubclass) {
                // No subclass found; return policy for c1
                return options().classPolicies.get(c1.getName());
            }
        }
        return null;
    }
}
