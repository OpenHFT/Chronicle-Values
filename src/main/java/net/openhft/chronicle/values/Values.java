/*
 *      Copyright (C) 2015, 2016  higherfrequencytrading.com
 *      Copyright (C) 2016 Roman Leventov
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
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