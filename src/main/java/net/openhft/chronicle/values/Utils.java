/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.util.Locale;

/**
 * Miscellaneous helper methods used during code generation.
 *
 * <p>The {@link #UNSAFE_CLASS} constant is resolved reflectively so that
 * Chronicle Values does not require {@code sun.misc.Unsafe} on the
 * compilation class-path. It exists purely for the generators; code
 * produced by the library must obtain the {@code Unsafe} instance via
 * {@code Jvm.theUnsafe} and should never reference this field directly.
 */
final class Utils {
    /**
     * Class object for {@code sun.misc.Unsafe}, loaded reflectively to avoid a
     * hard dependency on that class. Only the generators should reference this
     * constant.
     */
    static final Class<?> UNSAFE_CLASS;

    static {
        try {
            UNSAFE_CLASS = Class.forName("sun.misc.Unsafe");
        } catch (ClassNotFoundException cnfe) {
            throw new AssertionError(cnfe);
        }
    }

    private Utils() {
    }

    /**
     * Rounds {@code divident} up to the nearest multiple of {@code divisor}.
     *
     * @param divident the value to round
     * @param divisor  the alignment unit
     * @return the smallest multiple of {@code divisor} not less than {@code divident}
     */
    public static int roundUp(int divident, int divisor) {
        return ((divident + divisor - 1) / divisor) * divisor;
    }

    /**
     * Capitalises the first character of {@code s}.
     *
     * @param s the string to alter
     * @return {@code s} with the first character converted to upper case
     */
    static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Formats the value for code generation, appending {@code 'L'} if
     * it does not fit into an {@code int}.
     *
     * @param v the value to format
     * @return decimal representation with an optional {@code 'L'} suffix
     */
    static String formatIntOrLong(long v) {
        if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
            return v + "";
        return v + "L";
    }
}
