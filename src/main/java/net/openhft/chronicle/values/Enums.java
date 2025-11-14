/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.EnumSet;

/**
 * Utility methods used by generated code for enum fields. The generator caches
 * the array of constants for each enum so that values can be marshalled by
 * ordinal. The constants array is obtained by reflectively invoking the
 * package-private {@code EnumSet.getUniverse(Class)} method.
 */
public final class Enums {

    private static final Method getUniverse;

    static {
        try {
            getUniverse = doPrivileged(
                    (PrivilegedExceptionAction<Method>) () -> {
                        Method method =
                                EnumSet.class.getDeclaredMethod("getUniverse", Class.class);
                        method.setAccessible(true);
                        return method;
                    });
        } catch (PrivilegedActionException e) {
            throw new IllegalStateException("Unable to access EnumSet#getUniverse", e.getCause());
        }
    }

    private Enums() {
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
            throw new IllegalStateException("Unable to access enum constants", e);
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

    @SuppressWarnings({"deprecation", "removal"})
    private static <T> T doPrivileged(PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException {
        return java.security.AccessController.doPrivileged(action);
    }
}
