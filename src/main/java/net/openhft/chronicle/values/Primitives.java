/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
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

final class Primitives {

    private Primitives() {
    }

    static boolean isPrimitiveIntegerType(Class type) {
        return type == byte.class || type == short.class || type == char.class ||
                type == int.class || type == long.class;
    }

    static boolean isPrimitiveFloatingType(Class type) {
        return type == float.class || type == double.class;
    }

    static int widthInBits(Class type) {
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

    static Class boxed(Class type) {
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
