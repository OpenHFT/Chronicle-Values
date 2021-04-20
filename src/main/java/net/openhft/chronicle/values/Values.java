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

/**
 * This class is a central access point for loading generated heap and native Values.
 */
public final class Values {

    private Values() {
    }

    public static boolean isValueInterfaceOrImplClass(Class<?> valueTypeOrImplClass) {
        return ValueModel.isValueInterfaceOrImplClass(valueTypeOrImplClass);
    }

    /**
     * Equivalent for {@link #heapClassFor(Class) heapClassFor(valueType)}{@code .newInstance()}.
     *
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a heap implementation failed
     */
    public static <T> T newHeapInstance(Class<T> valueType) {
        try {
            return heapClassFor(valueType).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Equivalent for {@link #nativeClassFor nativeClassFor(valueType)}{@code .newInstance()}.
     *
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a native implementation failed
     */
    public static <T> T newNativeReference(Class<T> valueType) {
        try {
            return nativeClassFor(valueType).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Generates (if not yet) and returns a heap implementation for the given value interface.
     *
     * @param valueType the value interface to return a heap implementation for
     * @param <T>       the value interface as a type parameter
     * @return a heap implementation class for the given value interface
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a heap implementation failed
     */
    public static <T> Class<T> heapClassFor(Class<T> valueType) {
        //noinspection unchecked
        return ValueModel.acquire(valueType).heapClass();
    }

    /**
     * Generates (if not yet) and returns a native (flyweight) implementation for the given value
     * interface.
     *
     * @param valueType the value interface to return a native implementation for
     * @param <T>       the value interface as a type parameter
     * @return a native (flyweight) implementation class for the given value interface
     * @throws IllegalArgumentException      if the given {@code valueType} is not a value interface
     * @throws ImplGenerationFailedException if generation of a native implementation failed
     */
    public static <T> Class<T> nativeClassFor(Class<T> valueType) {
        //noinspection unchecked
        return ValueModel.acquire(valueType).nativeClass();
    }
}