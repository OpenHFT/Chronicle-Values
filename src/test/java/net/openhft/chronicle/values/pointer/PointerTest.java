/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.pointer;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

public class PointerTest extends ValuesTestCommon {

    private static long getAddress(Byteable byteable) {
        return requireNonNull(byteable.bytesStore()).addressForRead(byteable.offset());
    }

    @SuppressWarnings("unchecked")
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
        PointedInterface pointedFoo = getPointed();
        assertTrue(pointedFoo.offset() < 10_000, "pointedFoo: offset is within expected range");
        pointedFoo.setString("foo");
        long fooAddress = pointedFoo.address();
        PointedInterface pointedBar = getPointed();
        assertTrue(pointedBar.offset() < 10_000, "pointedBar: offset is within expected range");
        pointedBar.setString("bar");
        long barAddress = pointedBar.address();

        PointingInterface heapPointing = Values.newHeapInstance(PointingInterface.class);
        assertNull(heapPointing.getPoint(), "heap pointing: point starts unset");
        assertNull(heapPointing.getVolatilePoint(), "heap pointing: volatile point starts unset");
        heapPointing.setPoint(pointedFoo);
        assertTrue(heapPointing.getPoint().offset() < 10_000, "heap pointing: stored point offset is within range");
        // checks that heap object doesn't simply store reference to an object, only address
        assertNotSame(heapPointing.getPoint(), pointedFoo, "heap pointing: point is not the original instance");
        assertNotSame(heapPointing.getVolatilePoint(), pointedFoo, "heap pointing: volatile point is not the original instance");
        assertEquals(fooAddress, heapPointing.getPoint().address(), "heap pointing: point stores foo address");
        assertEquals(fooAddress, heapPointing.getVolatilePoint().address(), "heap pointing: volatile point stores foo address");

        // check setVolatile
        heapPointing.setVolatilePoint(pointedBar);
        assertNotSame(heapPointing.getPoint(), pointedBar, "heap pointing: point is not the original instance");
        assertEquals(barAddress, heapPointing.getPoint().address(), "heap pointing: point stores bar address");

        // check setOrdered
        heapPointing.setOrderedPoint(pointedFoo);
        assertNotSame(heapPointing.getVolatilePoint(), pointedFoo, "heap pointing: volatile point is not the original instance");
        assertEquals(fooAddress, heapPointing.getVolatilePoint().address(), "heap pointing: volatile point stores foo address");

        assertFalse(heapPointing.compareAndSwapPoint(pointedBar, pointedFoo), "CAS: expected failure for wrong current value");
        assertTrue(heapPointing.compareAndSwapPoint(pointedFoo, pointedBar), "CAS: expected success for matching current value");
        assertNotSame(heapPointing.getPoint(), pointedBar, "heap pointing: point is not the original instance");
        assertEquals(barAddress, heapPointing.getPoint().address(), "heap pointing: point stores bar address after CAS");

        Values.nativeClassFor(PointedInterface.class);
        requireNonNull(pointedFoo.bytesStore()).releaseLast();
        requireNonNull(pointedBar.bytesStore()).releaseLast();
    }
}
