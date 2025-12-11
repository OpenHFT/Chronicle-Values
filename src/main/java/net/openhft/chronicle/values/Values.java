/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.InvocationTargetException;

/**
 * Central access point for loading generated heap and native Values.
 * Provides convenient factory methods for their implementations.
 *
 * <p>Example:
 * <pre>{@code
 * Point ref = Values.newNativeReference(Point.class);
 * BytesStore<?, ?> bs = BytesStore.nativeStoreWithFixedCapacity(ref.maxSize());
 * ((Byteable) ref).bytesStore(bs, 0, ref.maxSize());
 * }</pre>
 */
public final class Values {

    private Values() {
    }

    /**
     * Determines whether the type is a value interface or its generated implementation.
     *
     * @param valueTypeOrImplClass candidate type
     * @return {@code true} if the type is a value interface or generated impl
     */
    public static boolean isValueInterfaceOrImplClass(Class<?> valueTypeOrImplClass) {
        return ValueModel.isValueInterfaceOrImplClass(valueTypeOrImplClass);
    }

    /**
     * Equivalent for {@link #heapClassFor(Class) heapClassFor(valueType)}{@code .newInstance()}.
     * Creates a stand-alone heap object whose state lives in ordinary Java memory.
     * This instance is not thread-safe unless the interface specifies otherwise.
     *
     * @param valueType value interface to instantiate
     * @param <T>       value type
     * @return heap instance
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
     * @param valueType value interface to instantiate
     * @param <T>       value type
     * @return native reference instance
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
