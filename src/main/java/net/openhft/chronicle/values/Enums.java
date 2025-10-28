/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

/**
 * Utility methods used by generated code for enum fields. The generator caches
 * the array of constants for each enum so that values can be marshalled by
 * ordinal. The constants array is obtained by reflectively invoking the
 * package-private {@code EnumSet.getUniverse(Class)} method.
 */

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.EnumSet;

public final class Enums {

    private static final Method getUniverse = initGetUniverse();

    private Enums() {
    }

    @SuppressWarnings({"removal", "deprecation"})
    private static Method initGetUniverse() {
        try {
            PrivilegedExceptionAction<Method> action = () -> {
                Method method = EnumSet.class.getDeclaredMethod("getUniverse", Class.class);
                method.setAccessible(true);
                return method;
            };
            try {
                Class<?> accessController = Class.forName("java.security.AccessController");
                Method doPrivileged = accessController.getMethod("doPrivileged", PrivilegedExceptionAction.class);
                return (Method) doPrivileged.invoke(null, action);
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                return action.run();
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getTargetException();
                if (cause instanceof PrivilegedActionException) {
                    throw (PrivilegedActionException) cause;
                }
                if (cause instanceof Exception) {
                    throw new RuntimeException(cause);
                }
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the constant array backing the supplied enum type.
     *
     * @param enumType the enum class
     * @param <E>      type of the enum
     * @return the array returned by {@code EnumSet.getUniverse(Class)}
     * @throws RuntimeException if reflective access fails
     */
    public static <E extends Enum<E>> E[] getUniverse(Class<E> enumType) {
        try {
            //noinspection unchecked
            return Jvm.uncheckedCast(getUniverse.invoke(null, enumType));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the number of constants declared by the enum.
     *
     * @param enumType the enum class
     * @param <E>      type of the enum
     * @return constant count
     */
    public static <E extends Enum<E>> int numberOfConstants(Class<E> enumType) {
        return getUniverse(enumType).length;
    }
}
