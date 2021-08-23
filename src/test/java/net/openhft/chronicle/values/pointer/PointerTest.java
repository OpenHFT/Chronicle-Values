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

package net.openhft.chronicle.values.pointer;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

public class PointerTest extends ValuesTestCommon {

    private static long getAddress(Byteable<?, ?> byteable) {
        return requireNonNull(byteable.bytesStore()).addressForRead(byteable.offset());
    }

    @NotNull
    private static PointedInterface getPointed() {
        PointedInterface pointed1 = Values.newNativeReference(PointedInterface.class);
        long pointedSize = pointed1.maxSize();
        //noinspection unchecked
        pointed1.bytesStore(Bytes.allocateDirect(pointedSize), 0, pointedSize);
        return pointed1;
    }

    @Test
    public void testPointer() {
        System.setProperty("chronicle.values.dumpCode", "true");

        PointedInterface pointedFoo = getPointed();
        assertTrue(pointedFoo.offset() < 10_000);
        pointedFoo.setString("foo");
        long fooAddress = pointedFoo.address();
        PointedInterface pointedBar = getPointed();
        assertTrue(pointedBar.offset() < 10_000);
        pointedBar.setString("bar");
        long barAddress = pointedBar.address();

        PointingInterface heapPointing = Values.newHeapInstance(PointingInterface.class);
        assertNull(heapPointing.getPoint());
        assertNull(heapPointing.getVolatilePoint());
        heapPointing.setPoint(pointedFoo);
        assertTrue(heapPointing.getPoint().offset() < 10_000);
        // checks that heap object doesn't simply store reference to an object, only address
        assertNotSame(heapPointing.getPoint(), pointedFoo);
        assertNotSame(heapPointing.getVolatilePoint(), pointedFoo);
        assertEquals(fooAddress, heapPointing.getPoint().address());
        assertEquals(fooAddress, heapPointing.getVolatilePoint().address());

        // check setVolatile
        heapPointing.setVolatilePoint(pointedBar);
        assertNotSame(heapPointing.getPoint(), pointedBar);
        assertEquals(barAddress, heapPointing.getPoint().address());

        // check setOrdered
        heapPointing.setOrderedPoint(pointedFoo);
        assertNotSame(heapPointing.getVolatilePoint(), pointedFoo);
        assertEquals(fooAddress, heapPointing.getVolatilePoint().address());

        assertFalse(heapPointing.compareAndSwapPoint(pointedBar, pointedFoo));
        assertTrue(heapPointing.compareAndSwapPoint(pointedFoo, pointedBar));
        assertNotSame(heapPointing.getPoint(), pointedBar);
        assertEquals(barAddress, heapPointing.getPoint().address());

        Values.nativeClassFor(PointedInterface.class);
        requireNonNull(pointedFoo.bytesStore()).releaseLast();
        requireNonNull(pointedBar.bytesStore()).releaseLast();
    }
}
