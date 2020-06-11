/*
 *      Copyright (C) 2015  higherfrequencytrading.com
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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static net.openhft.chronicle.bytes.NativeBytesStore.nativeStoreWithFixedCapacity;
import static org.junit.Assert.*;

public class CoreValuesTest extends ValuesTestCommon {

    @Test
    public void testHeapIntValue() {
        testIntValue(Values.newHeapInstance(IntValue.class));
    }

    @Test
    public void testNativeIntValue() {
        IntValue intValue = Values.newNativeReference(IntValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) intValue).maxSize());
        assertEquals(4, bs.capacity());
        ((Byteable) intValue).bytesStore(bs, 0, ((Byteable) intValue).maxSize());
        testIntValue(intValue);
    }

    public void testIntValue(IntValue v) {
        assertEquals(0, v.getValue());

        v.setValue(1);
        assertEquals(1, v.getValue());

        v.addValue(1);
        assertEquals(2, v.getValue());

        v.addAtomicValue(-1);
        assertEquals(1, v.getValue());

        assertTrue(v.compareAndSwapValue(1, 2));
        assertEquals(2, v.getValue());
        assertFalse(v.compareAndSwapValue(1, 2));
        assertEquals(2, v.getValue());

        v.setOrderedValue(3);
        assertEquals(3, v.getValue());
    }

    @Test
    public void testHeapUnsignedIntValue() {
        testUnsignedIntValue(Values.newHeapInstance(UnsignedIntValue.class));
    }

    @Test
    public void testNativeUnsignedIntValue() {
        UnsignedIntValue unsignedIntValue = Values.newNativeReference(UnsignedIntValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) unsignedIntValue).maxSize());
        assertEquals(4, bs.capacity());
        ((Byteable) unsignedIntValue).bytesStore(bs, 0, ((Byteable) unsignedIntValue).maxSize());
        testUnsignedIntValue(unsignedIntValue);
    }

    public void testUnsignedIntValue(UnsignedIntValue v) {
        assertEquals(0, v.getValue());

        v.setValue(1);
        assertEquals(1, v.getValue());

        v.addValue(1);
        assertEquals(2, v.getValue());
    }

    @Test
    public void testHeapByteValue() {
        testByteValue(Values.newHeapInstance(ByteValue.class));
    }

    @Test
    public void testNativeByteValue() {
        ByteValue byteValue = Values.newNativeReference(ByteValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) byteValue).maxSize());
        assertEquals(1, bs.capacity());
        ((Byteable) byteValue).bytesStore(bs, 0, ((Byteable) byteValue).maxSize());
        testByteValue(byteValue);
    }

    public void testByteValue(ByteValue v) {
        assertEquals(0, v.getValue());

        v.setValue((byte) 1);
        assertEquals(1, v.getValue());

        v.addValue((byte) 1);
        assertEquals(2, v.getValue());
    }

    @Test
    public void testHeapCharValue() {
        testCharValue(Values.newHeapInstance(CharValue.class));
    }

    @Test
    public void testNativeCharValue() {
        CharValue charValue = Values.newNativeReference(CharValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) charValue).maxSize());
        assertEquals(2, bs.capacity());
        ((Byteable) charValue).bytesStore(bs, 0, ((Byteable) charValue).maxSize());
        testCharValue(charValue);
    }

    public void testCharValue(CharValue v) {
        assertEquals(0, v.getValue());

        v.setValue((char) 1);
        assertEquals(1, v.getValue());
    }

    @Test
    public void testHeapLongValue() {
        testLongValue(Values.newHeapInstance(LongValue.class));
    }

    @Test
    public void testNativeLongValue() {
        LongValue longValue = Values.newNativeReference(LongValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) longValue).maxSize());
        assertEquals(8, bs.capacity());
        ((Byteable) longValue).bytesStore(bs, 0, ((Byteable) longValue).maxSize());
        testLongValue(longValue);
    }

    public void testLongValue(LongValue v) {
        assertEquals(0, v.getValue());

        v.setValue(1L);
        assertEquals(1, v.getValue());

        v.addValue(1);
        assertEquals(2, v.getValue());

        v.addAtomicValue(-1);
        assertEquals(1, v.getValue());

        assertTrue(v.compareAndSwapValue(1, 2));
        assertEquals(2, v.getValue());
        assertFalse(v.compareAndSwapValue(1, 2));
        assertEquals(2, v.getValue());

        v.setOrderedValue(3);
        assertEquals(3, v.getValue());
    }

    @Test
    public void testHeapFloatValue() {
        testFloatValue(Values.newHeapInstance(FloatValue.class));
    }

    @Test
    public void testNativeFloatValue() {
        FloatValue floatValue = Values.newNativeReference(FloatValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) floatValue).maxSize());
        assertEquals(4, bs.capacity());
        ((Byteable) floatValue).bytesStore(bs, 0, ((Byteable) floatValue).maxSize());
        testFloatValue(floatValue);
    }

    public void testFloatValue(FloatValue v) {
        assertTrue(0.0f == v.getValue());

        v.setValue(1.0f);
        assertTrue(1.0f == v.getValue());

        v.addValue(1.0f);
        assertTrue(1.0f + 1.0f == v.getValue());
        float v2 = v.getValue();

        v.addAtomicValue(-1.0f);
        assertTrue(v2 + (-1.0f) == v.getValue());

        v.setOrderedValue(3.0f);
        assertTrue(3.0f == v.getValue());
    }

    @Test
    public void testHeapDoubleValue() {
        testDoubleValue(Values.newHeapInstance(DoubleValue.class));
    }

    @Test
    public void testNativeDoubleValue() {
        DoubleValue doubleValue = newBackedNativeDoubleValue();
        testDoubleValue(doubleValue);
    }
    
    @NotNull
    private DoubleValue newBackedNativeDoubleValue() {
        DoubleValue doubleValue = Values.newNativeReference(DoubleValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) doubleValue).maxSize());
        assertEquals(8, bs.capacity());
        ((Byteable) doubleValue).bytesStore(bs, 0, ((Byteable) doubleValue).maxSize());
        return doubleValue;
    }

    @Test
    public void testDoubleValueEquals() {
        DoubleValue nativeDoubleValue = newBackedNativeDoubleValue();
        DoubleValue heapDoubleValue = Values.newHeapInstance(DoubleValue.class);
        nativeDoubleValue.setValue(11.0);
        heapDoubleValue.setValue(11.0);
        assertEquals(nativeDoubleValue, heapDoubleValue);
    }

    public void testDoubleValue(DoubleValue v) {
        assertTrue(0.0 == v.getValue());

        v.setValue(1.0);
        assertTrue(1.0 == v.getValue());

        v.addValue(1.0);
        assertTrue(1.0 + 1.0 == v.getValue());
        double v2 = v.getValue();

        v.addAtomicValue(-1.0);
        assertTrue(v2 + (-1.0) == v.getValue());

        v.setOrderedValue(3.0);
        assertTrue(3.0 == v.getValue());
    }

    @Test
    public void testHeapShortValue() {
        testShortValue(Values.newHeapInstance(ShortValue.class));
    }

    @Test
    public void testNativeShortValue() {
        ShortValue shortValue = Values.newNativeReference(ShortValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) shortValue).maxSize());
        assertEquals(2, bs.capacity());
        ((Byteable) shortValue).bytesStore(bs, 0, ((Byteable) shortValue).maxSize());
        testShortValue(shortValue);
    }

    public void testShortValue(ShortValue v) {
        assertEquals(0, v.getValue());

        v.setValue((short) 1);
        assertEquals(1, v.getValue());

        v.addValue((short) 1);
        assertEquals(2, v.getValue());
    }

    @Test
    public void testHeapBooleanValue() {
        testBooleanValue(Values.newHeapInstance(BooleanValue.class));
    }

    @Test
    public void testNativeBooleanValue() {
        BooleanValue booleanValue = Values.newNativeReference(BooleanValue.class);
        BytesStore bs = nativeStoreWithFixedCapacity(((Byteable) booleanValue).maxSize());
        assertEquals(1, bs.capacity());
        ((Byteable) booleanValue).bytesStore(bs, 0, ((Byteable) booleanValue).maxSize());
        testBooleanValue(booleanValue);
    }

    public void testBooleanValue(BooleanValue v) {
        assertFalse(v.getValue());

        v.setValue(true);
        assertTrue(v.getValue());
    }
}
