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

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.InvocationTargetException;

/**
 * Central access point for loading generated heap and native Values.
 * Provides convenient factory methods for their implementations.
 */
public final class Values {

    private Values() {
    }

    public static boolean isValueInterfaceOrImplClass(Class<?> valueTypeOrImplClass) {
        return ValueModel.isValueInterfaceOrImplClass(valueTypeOrImplClass);
    }

    /**
     * Equivalent for {@link #heapClassFor(Class) heapClassFor(valueType)}{@code .newInstance()}.
     * Creates a stand-alone heap object whose state lives in ordinary Java memory.
     * This instance is not thread-safe unless the interface specifies otherwise.
     *
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a heap implementation failed
     */
    public static <T> T newHeapInstance(Class<T> valueType) {
        try {
            return heapClassFor(valueType).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Equivalent for {@link #nativeClassFor nativeClassFor(valueType)}{@code .newInstance()}.
     * Creates a flyweight reference to off-heap memory. Point it at a {@code BytesStore} before use
     * and do not share between threads without external synchronisation.
     *
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a native implementation failed
     */
    public static <T> T newNativeReference(Class<T> valueType) {
        try {
            return nativeClassFor(valueType).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Generates (if not yet) and returns a heap implementation for the given value interface.
     * The call may trigger on-the-fly compilation of the generated class.
     *
     * @param valueType the value interface to return a heap implementation for
     * @param <T>       the value interface as a type parameter
     * @return a heap implementation class for the given value interface
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a heap implementation failed
     */
    public static <T> Class<T> heapClassFor(Class<T> valueType) {
        //noinspection unchecked
        return Jvm.uncheckedCast(ValueModel.acquire(valueType).heapClass());
    }

    /**
     * Generates (if not yet) and returns a native (flyweight) implementation for the given value
     * interface. The call may trigger on-the-fly compilation of the generated class.
     *
     * @param valueType the value interface to return a native implementation for
     * @param <T>       the value interface as a type parameter
     * @return a native (flyweight) implementation class for the given value interface
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a native implementation failed
     */
    public static <T> Class<T> nativeClassFor(Class<T> valueType) {
        //noinspection unchecked
        return Jvm.uncheckedCast(ValueModel.acquire(valueType).nativeClass());
    }
}
