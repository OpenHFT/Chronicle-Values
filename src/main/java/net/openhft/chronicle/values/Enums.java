/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
import java.util.EnumSet;

public final class Enums {

    private static final Method getUniverse;

    static {
        try {
            getUniverse = EnumSet.class.getDeclaredMethod("getUniverse", Class.class);
            getUniverse.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
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
