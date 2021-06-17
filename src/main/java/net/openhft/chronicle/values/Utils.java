/*
 * Copyright 2016-2021 chronicle.software
 *
 * https://chronicle.software
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

final class Utils {
    static final Class UNSAFE_CLASS;

    static {
        try {
            UNSAFE_CLASS = Class.forName("sun.misc.Unsafe");
        } catch (ClassNotFoundException cnfe) {
            throw new AssertionError(cnfe);
        }
    }

    private Utils() {
    }

    public static int roundUp(int divident, int divisor) {
        return ((divident + divisor - 1) / divisor) * divisor;
    }

    static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static String formatIntOrLong(long v) {
        if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
            return v + "";
        return v + "L";
    }
}
