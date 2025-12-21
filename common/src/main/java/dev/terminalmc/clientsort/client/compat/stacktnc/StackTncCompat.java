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

package dev.terminalmc.clientsort.client.compat.stacktnc;

import dev.terminalmc.clientsort.client.ClientSort;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StackTncCompat {

    public static final String MOD_NAME = "Stack to Nearby Chests";

    public static final String LOCKED_SLOTS_CLASS = "io.github.xiaocihua.stacktonearbychests.LockedSlots";
    public static final String IS_LOCKED_METHOD = "isLocked";
    public static final Class<?>[] IS_LOCKED_PARAMS = {Slot.class};

    private static boolean hasFailed = false;

    private static Method isLockedMethod = null;

    /**
     * @param slot the slot to check.
     * @return {@code true} if the slot is valid, locked, and the bypass is not active.
     */
    public static boolean isLocked(Slot slot) {
        if (hasFailed)
            return false;
        if (!(slot.container instanceof Inventory))
            return false;

        return checkStatic(slot);
    }

    public static boolean checkStatic(Slot slot) {
        try {
            // Load class and find method
            if (isLockedMethod == null) {
                Class<?> clazz = Class.forName(
                        LOCKED_SLOTS_CLASS,
                        false,
                        Thread.currentThread().getContextClassLoader()
                );
                isLockedMethod = clazz.getMethod(IS_LOCKED_METHOD, IS_LOCKED_PARAMS);
            }

            // Invoke static
            Object result = isLockedMethod.invoke(null, slot);
            if (result instanceof Boolean locked)
                return locked;
            else
                throw new ClassCastException();

        } catch (IllegalAccessException ignored) {
            ClientSort.LOG.info(
                    "{} could not be accessed: compat is now disabled",
                    MOD_NAME
            );
        } catch (ClassNotFoundException ignored) {
            ClientSort.LOG.info(
                    "{} did not provide expected class: compat is now disabled",
                    MOD_NAME
            );
        } catch (NoSuchMethodException ignored) {
            ClientSort.LOG.info(
                    "{} did not provide expected method: compat is now disabled",
                    MOD_NAME
            );
        } catch (ClassCastException ignored) {
            ClientSort.LOG.info(
                    "{} did not provide expected return type: compat is now disabled",
                    MOD_NAME
            );
        } catch (InvocationTargetException e) {
            ClientSort.LOG.info(
                    "{} threw an exception: compat is now disabled: {}",
                    e.getMessage()
            );
        }
        hasFailed = true;
        return false;
    }
}
