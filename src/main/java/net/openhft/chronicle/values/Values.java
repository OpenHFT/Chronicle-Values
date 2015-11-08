/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.values;

/**
 * This class is a central access point for loading generated heap and native Values.
 */
public final class Values {

    public static <T> T newHeapInstance(Class<T> valueType) {
        try {
            return heapClassFor(valueType).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T newNativeReference(Class<T> valueType) {
        try {
            return nativeClassFor(valueType).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> Class<T> heapClassFor(Class<T> valueType) {
        //noinspection unchecked
        return ValueModel.acquire(valueType).heapClass();
    }

    public static <T> Class<T> nativeClassFor(Class<T> valueType) {
        //noinspection unchecked
        return ValueModel.acquire(valueType).nativeClass();
    }

    private Values() {}
}