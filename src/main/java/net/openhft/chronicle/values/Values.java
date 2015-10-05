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

import net.openhft.chronicle.bytes.NativeBytesStore;

import java.util.WeakHashMap;

/**
 * This class is a central access point for loading generated on-heap and off heap collections.
 */
public final class Values {

    // the weak hash map is required as the class loader could go away without notice e.g. in OSGi
    private static final WeakHashMap<ClassLoader, ValueClassCache> cacheMap =
            new WeakHashMap<>();

    public static <T> T newInstance(Class<T> interfaceClass) {
        ValueClassCache valueClassCache = acquireCache(interfaceClass);
        return valueClassCache.newInstance(interfaceClass);
    }

    public static <T> T newDirectReference(Class<T> interfaceClass) {
        ValueClassCache valueClassCache = acquireCache(interfaceClass);
        return valueClassCache.newDirectReference(interfaceClass);
    }

    public static <T> T newDirectInstance(Class<T> interfaceClass) {
        T t = newDirectReference(interfaceClass);
        net.openhft.chronicle.bytes.Byteable b = (net.openhft.chronicle.bytes.Byteable) t;
        b.bytesStore(NativeBytesStore.nativeStoreWithFixedCapacity(b.maxSize()), 0, b.maxSize());
        return t;
    }

    public static <T> Class<T> heapClassFor(Class<T> interfaceClass) {
        ValueClassCache valueClassCache = acquireCache(interfaceClass);
        return valueClassCache.heapClassFor(interfaceClass);
    }

    public static <T> Class<T> directClassFor(Class<T> interfaceClass) {
        ValueClassCache valueClassCache = acquireCache(interfaceClass);
        return valueClassCache.directClassFor(interfaceClass);
    }

    private static <T> ValueClassCache acquireCache(Class<T> interfaceClass) {
        ClassLoader classLoader = interfaceClass.getClassLoader();
        ValueClassCache valueClassCache;
        synchronized (cacheMap) {
            valueClassCache = cacheMap.get(classLoader);
            if (valueClassCache == null)
                cacheMap.put(classLoader, valueClassCache = new ValueClassCache());
        }
        return valueClassCache;
    }

    private Values() {}
}
