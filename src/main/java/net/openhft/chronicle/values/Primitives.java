//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

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
 * Utility methods for Java primitive types.
 *
 * <p>A boolean is treated as occupying one bit when calculating widths. This
 * mirrors the layout used by value classes.</p>
 *
 * <p>The {@link #boxed(Class)} helper returns the wrapper type for a primitive
 * and is used in generated code that requires a boxed reference.</p>
 */

final class Primitives {

    private Primitives() {
    }

    /**
     * Tests whether the supplied class represents one of the primitive integer
     * types.
     *
     * @param type class under test, must not be {@code null}
     * @return {@code true} if the type is byte, short, char, int or long
     */
    static boolean isPrimitiveIntegerType(Class<?> type) {
        return type == byte.class || type == short.class || type == char.class ||
                type == int.class || type == long.class;
    }

    /**
     * Tests whether the supplied class represents a primitive floating type.
     *
     * @param type class under test, must not be {@code null}
     * @return {@code true} if the type is float or double
     */
    static boolean isPrimitiveFloatingType(Class<?> type) {
        return type == float.class || type == double.class;
    }

    /**
     * Returns the number of bits needed to store the supplied primitive type.
     *
     * @param type primitive class to query, must not be {@code null}
     * @return width in bits, for example {@code 16} for {@code char}
     * @throws AssertionError if the argument is not a primitive type
     */
    static int widthInBits(Class<?> type) {
        if (type == boolean.class)
            return 1;
        if (type == byte.class)
            return 8;
        if (type == char.class || type == short.class)
            return 16;
        if (type == int.class || type == float.class)
            return 32;
        if (type == long.class || type == double.class)
            return 64;
        throw new AssertionError(type + " is not a primitive type");
    }

    /**
     * Maps a primitive type to its boxed counterpart.
     *
     * @param type primitive class to box, must not be {@code null}
     * @return wrapper type such as {@code Integer.class}
     * @throws AssertionError if the argument is not a primitive type
     */
    static Class<?> boxed(Class<?> type) {
        if (type == boolean.class)
            return Boolean.class;
        if (type == byte.class)
            return Byte.class;
        if (type == char.class)
            return Character.class;
        if (type == short.class)
            return Short.class;
        if (type == int.class)
            return Integer.class;
        if (type == long.class)
            return Long.class;
        if (type == float.class)
            return Float.class;
        if (type == double.class)
            return Double.class;
        throw new AssertionError("Expected primitive type: " + type);
    }
}
