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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static net.openhft.chronicle.bytes.BytesStore.nativeStoreWithFixedCapacity;
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
        bs.releaseLast();
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
        bs.releaseLast();
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
        bs.releaseLast();
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
        bs.releaseLast();
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
        try (LongValue longValue = Values.newNativeReference(LongValue.class)) {
            Byteable longByteableValue = (Byteable) longValue;
            BytesStore bs = nativeStoreWithFixedCapacity(longByteableValue.maxSize());
            assertEquals(8, bs.capacity());
            longByteableValue.bytesStore(bs, 0, longByteableValue.maxSize());
            testLongValue(longValue);
            bs.releaseLast();
        }
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
        bs.releaseLast();
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
        ((Byteable) doubleValue).bytesStore().releaseLast();
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
        ((Byteable) nativeDoubleValue).bytesStore().releaseLast();
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
        bs.releaseLast();
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
        bs.releaseLast();
    }

    public void testBooleanValue(BooleanValue v) {
        assertFalse(v.getValue());

        v.setValue(true);
        assertTrue(v.getValue());
    }
}
