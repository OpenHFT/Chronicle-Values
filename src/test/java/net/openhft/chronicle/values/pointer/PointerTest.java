/*
 *      Copyright (C) 2015-2020 chronicle.software
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

package net.openhft.chronicle.values.pointer;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class PointerTest extends ValuesTestCommon {

    private static long getAddress(Byteable byteable) {
        return byteable.bytesStore().addressForRead(byteable.offset());
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
        PointedInterface pointedFoo = getPointed();
        pointedFoo.setString("foo");
        long fooAddress = getAddress(pointedFoo);
        PointedInterface pointedBar = getPointed();
        pointedBar.setString("bar");
        long barAddress = getAddress(pointedBar);

        System.setProperty("chronicle.values.dumpCode", "true");

        PointingInterface heapPointing = Values.newHeapInstance(PointingInterface.class);
        assertNull(heapPointing.getPoint());
        assertNull(heapPointing.getVolatilePoint());
        heapPointing.setPoint(pointedFoo);
        // checks that heap object doesn't simply store reference to an object, only address
        assertFalse(heapPointing.getPoint() == pointedFoo);
        assertFalse(heapPointing.getVolatilePoint() == pointedFoo);
        assertEquals(fooAddress, getAddress(heapPointing.getPoint()));
        assertEquals(fooAddress, getAddress(heapPointing.getVolatilePoint()));

        // check setVolatile
        heapPointing.setVolatilePoint(pointedBar);
        assertFalse(heapPointing.getPoint() == pointedBar);
        assertEquals(barAddress, getAddress(heapPointing.getPoint()));

        // check setOrdered
        heapPointing.setOrderedPoint(pointedFoo);
        assertFalse(heapPointing.getVolatilePoint() == pointedFoo);
        assertEquals(fooAddress, getAddress(heapPointing.getVolatilePoint()));

        assertFalse(heapPointing.compareAndSwapPoint(pointedBar, pointedFoo));
        assertTrue(heapPointing.compareAndSwapPoint(pointedFoo, pointedBar));
        assertFalse(heapPointing.getPoint() == pointedBar);
        assertEquals(barAddress, getAddress(heapPointing.getPoint()));

        Values.nativeClassFor(PointedInterface.class);
        pointedFoo.bytesStore().releaseLast();
        pointedBar.bytesStore().releaseLast();
    }
}
