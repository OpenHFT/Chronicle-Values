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

    public static <E extends Enum<E>> E[] getUniverse(Class<E> enumType) {
        try {
            //noinspection unchecked
            return (E[]) getUniverse.invoke(null, enumType);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static <E extends Enum<E>> int numberOfConstants(Class<E> enumType) {
        return getUniverse(enumType).length;
    }
}
