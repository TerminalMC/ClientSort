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

package dev.terminalmc.clientsort.network.handler.validate;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.config.ClassPolicy;
import dev.terminalmc.clientsort.config.ServerConfig;
import dev.terminalmc.clientsort.exception.PayloadHandlerException;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.config.ServerConfig.serverOptions;

public class PolicyManager {

    private static final Set<Class<?>> policyClasses = new LinkedHashSet<>();

    private PolicyManager() {
    }

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

    public static void setPolicy(ClassPolicy classPolicy) {
        ClassPolicy existingPolicy = serverOptions().classPolicies.get(classPolicy.className);
        if (existingPolicy != null) {
            existingPolicy.setFrom(classPolicy);
            ClientSort.LOG.info(
                    "Updated the class policy for '{}' due to operation failure",
                    classPolicy.className
            );
        } else {
            serverOptions().classPolicies.put(classPolicy.className, classPolicy);
            ClientSort.LOG.info(
                    "Added a new class policy for '{}' due to operation failure",
                    classPolicy.className
            );
        }
        ServerConfig.save();
    }

    /**
     * @throws PayloadHandlerException if the server policy disallows the operation.
     */
    public static void checkPolicy(Class<?> cls, Function<ClassPolicy, Boolean> op)
            throws PayloadHandlerException {
        ClassPolicy configClassPolicy = getClassPolicy(cls);
        if (configClassPolicy != null && !op.apply(configClassPolicy)) {
            throw new PayloadHandlerException(String.format(
                    "Server policy does not allow this operation for class '%s'!",
                    cls.getName()
            ));
        }
    }

    /**
     * @return the lowest-degree matching item for the specified class, if any exists.
     */
    private static ClassPolicy getClassPolicy(Class<?> cls) {
        // Check for a perfect match
        ClassPolicy layout = serverOptions().classPolicies.get(cls.getName());
        if (layout != null)
            return layout;

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
                // No subclass found; return layout for c1
                return serverOptions().classPolicies.get(c1.getName());
            }
        }
        return null;
    }
}
