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

package dev.terminalmc.clientsort.client.compat.itemlocks;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ItemLocksCompat {

    public static final String MOD_NAME = "ItemLocks";

    public static final String KEY_BINDINGS_CLASS = "com.kirdow.itemlocks.client.input.KeyBindings";
    public static final String IS_BYPASS_METHOD = "isBypass";
    public static final Class<?>[] IS_BYPASS_PARAMS = {};

    public static final String COMPONENTS_CLASS = "com.kirdow.itemlocks.proxy.Components";
    public static final String GET_COMPONENT_METHOD = "getComponent";
    public static final Class<?>[] GET_COMPONENT_PARAMS = {Class.class};

    public static final String LOCK_MANAGER_CLASS = "com.kirdow.itemlocks.client.LockManager";
    public static final String IS_LOCKED_SLOT_RAW_METHOD = "isLockedSlotRaw";
    public static final Class<?>[] IS_LOCKED_SLOT_RAW_PARAMS = {int.class};

    private static boolean hasFailed = false;

    private static Method isBypassMethod = null;

    private static Object lockManagerInstance = null;
    private static Method isLockedSlotRawMethod = null;

    /**
     * @param slot the slot to check.
     * @return {@code true} if the slot is valid, locked, and the bypass is not active.
     */
    public static boolean isLocked(Slot slot) {
        if (hasFailed)
            return false;
        if (!(slot.container instanceof Inventory))
            return false;

        int index = adjustForInventory(((ISlot) slot).clientsort$getIndexInContainer());
        return checkStatic(index);
    }

    /**
     * Moves the hotbar from 0-8 to 27-35.
     */
    private static int adjustForInventory(int slot) {
        if (0 <= slot && slot <= 8) {
            return slot + 27;
        } else if (9 <= slot && slot <= 35) {
            return slot - 9;
        } else {
            return slot;
        }
    }

    public static boolean checkStatic(int index) {
        try {
            // Check bypass first, since it's simpler
            if (isBypassMethod == null) {
                // Load class and find method
                Class<?> keyBindingsClass = Class.forName(
                        KEY_BINDINGS_CLASS,
                        false,
                        Thread.currentThread().getContextClassLoader()
                );
                isBypassMethod = keyBindingsClass.getMethod(IS_BYPASS_METHOD, IS_BYPASS_PARAMS);
            }

            // Invoke static
            Object isBypassResult = isBypassMethod.invoke(null);
            if (isBypassResult instanceof Boolean bypass) {
                if (bypass)
                    return true;
            } else {
                throw new ClassCastException();
            }

            // Not bypassing, so now check locked
            if (lockManagerInstance == null || isLockedSlotRawMethod == null) {
                // Load components class and find method
                Class<?> componentsClass = Class.forName(
                        COMPONENTS_CLASS,
                        false,
                        Thread.currentThread().getContextClassLoader()
                );
                Method getComponentMethod = componentsClass.getMethod(
                        GET_COMPONENT_METHOD,
                        GET_COMPONENT_PARAMS
                );

                // Load lock manager class and get its instance and method
                Class<?> lockManagerClass = Class.forName(
                        LOCK_MANAGER_CLASS,
                        false,
                        Thread.currentThread().getContextClassLoader()
                );
                lockManagerInstance = getComponentMethod.invoke(null, lockManagerClass);
                if (lockManagerInstance == null) {
                    throw new ClassCastException();
                }

                isLockedSlotRawMethod = lockManagerClass.getMethod(
                        IS_LOCKED_SLOT_RAW_METHOD,
                        IS_LOCKED_SLOT_RAW_PARAMS
                );
            }

            // Invoke static
            Object isLockedResult = isLockedSlotRawMethod.invoke(lockManagerInstance, index);
            if (isLockedResult instanceof Boolean locked) {
                return locked;
            } else {
                throw new ClassCastException();
            }

        } catch (IllegalAccessException e) {
            ClientSort.LOG.info(
                    "{} could not be accessed - compat is now disabled: {}",
                    MOD_NAME,
                    e.getMessage()
            );
        } catch (ClassNotFoundException e) {
            ClientSort.LOG.info(
                    "{} did not provide expected class - compat is now disabled: {}",
                    MOD_NAME,
                    e.getMessage()
            );
        } catch (NoSuchMethodException e) {
            ClientSort.LOG.info(
                    "{} did not provide expected method - compat is now disabled: {}",
                    MOD_NAME,
                    e.getMessage()
            );
        } catch (ClassCastException e) {
            ClientSort.LOG.info(
                    "{} did not provide expected return type - compat is now disabled: {}",
                    MOD_NAME,
                    e.getMessage()
            );
        } catch (InvocationTargetException e) {
            ClientSort.LOG.info(
                    "{} threw an exception - compat is now disabled: {}",
                    MOD_NAME,
                    e.getMessage()
            );
        }
        hasFailed = true;
        return false;
    }
}
