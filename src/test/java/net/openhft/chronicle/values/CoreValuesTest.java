/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.bytes.BytesStore.nativeStoreWithFixedCapacity;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CoreValuesTest extends ValuesTestCommon {

    /**
     * Exercises {@link IntValue} when backed by normal heap memory.
     */
    @Test
    public void testHeapIntValue() {
        testIntValue(Values.newHeapInstance(IntValue.class));
    }

    /**
     * Exercises {@link IntValue} using a {@link BytesStore}-backed native instance.
     */
    @Test
    public void testNativeIntValue() {
        IntValue intValue = Values.newNativeReference(IntValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) intValue).maxSize());
        assertEquals(4, bs.capacity());
        ((Byteable) intValue).bytesStore(bs, 0, ((Byteable) intValue).maxSize());
        testIntValue(intValue);
        bs.releaseLast();
    }

    /**
     * Shared assertions for any {@link IntValue} implementation.
     */
    private void testIntValue(IntValue v) {
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

    /**
     * Uses a heap-backed {@link UnsignedIntValue} to verify unsigned arithmetic behaviour.
     */
    @Test
    public void testHeapUnsignedIntValue() {
        testUnsignedIntValue(Values.newHeapInstance(UnsignedIntValue.class));
    }

    /**
     * The native form of {@link UnsignedIntValue} should mirror the heap version.
     */
    @Test
    public void testNativeUnsignedIntValue() {
        UnsignedIntValue unsignedIntValue = Values.newNativeReference(UnsignedIntValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) unsignedIntValue).maxSize());
        assertEquals(4, bs.capacity());
        ((Byteable) unsignedIntValue).bytesStore(bs, 0, ((Byteable) unsignedIntValue).maxSize());
        testUnsignedIntValue(unsignedIntValue);
        bs.releaseLast();
    }

    /**
     * Helper for unsigned integer operations across heap and native values.
     */
    private void testUnsignedIntValue(UnsignedIntValue v) {
        assertEquals(0, v.getValue());

        v.setValue(1);
        assertEquals(1, v.getValue());

        v.addValue(1);
        assertEquals(2, v.getValue());
    }

    /**
     * Validates {@link ByteValue} operations for a heap instance.
     */
    @Test
    public void testHeapByteValue() {
        testByteValue(Values.newHeapInstance(ByteValue.class));
    }

    /**
     * Runs the same assertions against a native {@link ByteValue}.
     */
    @Test
    public void testNativeByteValue() {
        ByteValue byteValue = Values.newNativeReference(ByteValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) byteValue).maxSize());
        assertEquals(1, bs.capacity());
        ((Byteable) byteValue).bytesStore(bs, 0, ((Byteable) byteValue).maxSize());
        testByteValue(byteValue);
        bs.releaseLast();
    }

    /**
     * Core checks for {@link ByteValue} implementations.
     */
    private void testByteValue(ByteValue v) {
        assertEquals(0, v.getValue());

        v.setValue((byte) 1);
        assertEquals(1, v.getValue());

        v.addValue((byte) 1);
        assertEquals(2, v.getValue());
    }

    /**
     * Checks that a heap-backed {@link CharValue} supports basic set and get.
     */
    @Test
    public void testHeapCharValue() {
        testCharValue(Values.newHeapInstance(CharValue.class));
    }

    /**
     * Ensures the native version of {@link CharValue} behaves the same as the heap one.
     */
    @Test
    public void testNativeCharValue() {
        CharValue charValue = Values.newNativeReference(CharValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) charValue).maxSize());
        assertEquals(2, bs.capacity());
        ((Byteable) charValue).bytesStore(bs, 0, ((Byteable) charValue).maxSize());
        testCharValue(charValue);
        bs.releaseLast();
    }

    /**
     * Helper for verifying {@link CharValue} semantics.
     */
    private void testCharValue(CharValue v) {
        assertEquals(0, v.getValue());

        v.setValue((char) 1);
        assertEquals(1, v.getValue());
    }

    /**
     * Uses a heap-backed {@link LongValue} to exercise atomic operations.
     */
    @Test
    public void testHeapLongValue() {
        testLongValue(Values.newHeapInstance(LongValue.class));
    }

    /**
     * Runs the same checks on a native {@link LongValue} instance.
     */
    @Test
    public void testNativeLongValue() {
        try (LongValue longValue = Values.newNativeReference(LongValue.class)) {
            Byteable longByteableValue = (Byteable) longValue;
            BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(longByteableValue.maxSize());
            assertEquals(8, bs.capacity());
            longByteableValue.bytesStore(bs, 0, longByteableValue.maxSize());
            testLongValue(longValue);
            bs.releaseLast();
        }
    }

    /**
     * Common logic for both heap and native {@link LongValue} checks.
     */
    private void testLongValue(LongValue v) {
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

    /**
     * Verifies {@link FloatValue} behaviour in heap form.
     */
    @Test
    public void testHeapFloatValue() {
        testFloatValue(Values.newHeapInstance(FloatValue.class));
    }

    /**
     * Executes the same checks using a native {@link FloatValue}.
     */
    @Test
    public void testNativeFloatValue() {
        FloatValue floatValue = Values.newNativeReference(FloatValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) floatValue).maxSize());
        assertEquals(4, bs.capacity());
        ((Byteable) floatValue).bytesStore(bs, 0, ((Byteable) floatValue).maxSize());
        testFloatValue(floatValue);
        bs.releaseLast();
    }

    /**
     * Assertions shared by heap and native {@link FloatValue} tests.
     */
    private void testFloatValue(FloatValue v) {
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

    /**
     * Validates {@link DoubleValue} when allocated on the heap.
     */
    @Test
    public void testHeapDoubleValue() {
        testDoubleValue(Values.newHeapInstance(DoubleValue.class));
    }

    /**
     * Runs the same assertions on a native {@link DoubleValue} backed by a {@link BytesStore}.
     */
    @Test
    public void testNativeDoubleValue() {
        DoubleValue doubleValue = newBackedNativeDoubleValue();
        testDoubleValue(doubleValue);
        ((Byteable) doubleValue).bytesStore().releaseLast();
    }

    @NotNull
    private DoubleValue newBackedNativeDoubleValue() {
        DoubleValue doubleValue = Values.newNativeReference(DoubleValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) doubleValue).maxSize());
        assertEquals(8, bs.capacity());
        ((Byteable) doubleValue).bytesStore(bs, 0, ((Byteable) doubleValue).maxSize());
        return doubleValue;
    }

    /**
     * Confirms that heap and native double values compare equal when holding the same data.
     */
    @Test
    public void testDoubleValueEquals() {
        DoubleValue nativeDoubleValue = newBackedNativeDoubleValue();
        DoubleValue heapDoubleValue = Values.newHeapInstance(DoubleValue.class);
        nativeDoubleValue.setValue(11.0);
        heapDoubleValue.setValue(11.0);
        assertEquals(nativeDoubleValue, heapDoubleValue);
        ((Byteable) nativeDoubleValue).bytesStore().releaseLast();
    }

    /**
     * Common set of assertions for {@link DoubleValue} instances.
     */
    private void testDoubleValue(DoubleValue v) {
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

    /**
     * Checks a heap-backed {@link ShortValue} for simple arithmetic operations.
     */
    @Test
    public void testHeapShortValue() {
        testShortValue(Values.newHeapInstance(ShortValue.class));
    }

    /**
     * Verifies that the native version of {@link ShortValue} matches the heap behaviour.
     */
    @Test
    public void testNativeShortValue() {
        ShortValue shortValue = Values.newNativeReference(ShortValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) shortValue).maxSize());
        assertEquals(2, bs.capacity());
        ((Byteable) shortValue).bytesStore(bs, 0, ((Byteable) shortValue).maxSize());
        testShortValue(shortValue);
        bs.releaseLast();
    }

    /**
     * Helper for {@link ShortValue} arithmetic tests.
     */
    private void testShortValue(ShortValue v) {
        assertEquals(0, v.getValue());

        v.setValue((short) 1);
        assertEquals(1, v.getValue());

        v.addValue((short) 1);
        assertEquals(2, v.getValue());
    }

    /**
     * Checks a heap-backed {@link BooleanValue} toggles correctly.
     */
    @Test
    public void testHeapBooleanValue() {
        testBooleanValue(Values.newHeapInstance(BooleanValue.class));
    }

    /**
     * Performs the same toggle test on a native {@link BooleanValue}.
     */
    @Test
    public void testNativeBooleanValue() {
        BooleanValue booleanValue = Values.newNativeReference(BooleanValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) booleanValue).maxSize());
        assertEquals(1, bs.capacity());
        ((Byteable) booleanValue).bytesStore(bs, 0, ((Byteable) booleanValue).maxSize());
        testBooleanValue(booleanValue);
        bs.releaseLast();
    }

    /**
     * Helper used by both heap and native boolean tests.
     */
    private void testBooleanValue(BooleanValue v) {
        assertFalse(v.getValue());

        v.setValue(true);
        assertTrue(v.getValue());
    }
}
