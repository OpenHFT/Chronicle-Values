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
        IntValue intValue = Values.newHeapInstance(IntValue.class);
        assertNotNull(intValue, "heap-allocated int value instance should be created successfully");
        testIntValue(intValue);
    }

    /**
     * Exercises {@link IntValue} using a {@link BytesStore}-backed native instance.
     */
    @Test
    public void testNativeIntValue() {
        IntValue intValue = Values.newNativeReference(IntValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) intValue).maxSize());
        assertEquals(4, bs.capacity(), "native int value should require 4-byte BytesStore capacity");
        ((Byteable) intValue).bytesStore(bs, 0, ((Byteable) intValue).maxSize());
        testIntValue(intValue);
        bs.releaseLast();
    }

    /**
     * Shared assertions for any {@link IntValue} implementation.
     */
    // CPD-OFF - shared lifecycle assertions between int/long values
    private void testIntValue(IntValue v) {
        assertEquals(0, v.getValue(), "int value should initialize to zero on creation");

        v.setValue(1);
        assertEquals(1, v.getValue(), "setValue should update int value to specified amount");

        v.addValue(1);
        assertEquals(2, v.getValue(), "addValue should increment int value by specified amount");

        v.addAtomicValue(-1);
        assertEquals(1, v.getValue(), "addAtomicValue should atomically decrement int value");

        assertTrue(v.compareAndSwapValue(1, 2), "compareAndSwapValue should succeed when current value matches expected");
        assertEquals(2, v.getValue(), "compareAndSwapValue should apply new value when swap succeeds");
        assertFalse(v.compareAndSwapValue(1, 2), "compareAndSwapValue should fail when current value does not match expected");
        assertEquals(2, v.getValue(), "compareAndSwapValue should leave value unchanged when swap fails");

        v.setOrderedValue(3);
        assertEquals(3, v.getValue(), "setOrderedValue should update int value with memory ordering semantics");
    }

    /**
     * Uses a heap-backed {@link UnsignedIntValue} to verify unsigned arithmetic behaviour.
     */
    @Test
    public void testHeapUnsignedIntValue() {
        UnsignedIntValue unsignedIntValue = Values.newHeapInstance(UnsignedIntValue.class);
        assertNotNull(unsignedIntValue, "heap-allocated unsigned int value instance should be created successfully");
        testUnsignedIntValue(unsignedIntValue);
    }

    /**
     * The native form of {@link UnsignedIntValue} should mirror the heap version.
     */
    @Test
    public void testNativeUnsignedIntValue() {
        UnsignedIntValue unsignedIntValue = Values.newNativeReference(UnsignedIntValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) unsignedIntValue).maxSize());
        assertEquals(4, bs.capacity(), "native unsigned int value should require 4-byte BytesStore capacity");
        ((Byteable) unsignedIntValue).bytesStore(bs, 0, ((Byteable) unsignedIntValue).maxSize());
        testUnsignedIntValue(unsignedIntValue);
        bs.releaseLast();
    }

    /**
     * Helper for unsigned integer operations across heap and native values.
     */
    private void testUnsignedIntValue(UnsignedIntValue v) {
        assertEquals(0, v.getValue(), "unsigned int value should initialize to zero on creation");

        v.setValue(1);
        assertEquals(1, v.getValue(), "setValue should update unsigned int value to specified amount");

        v.addValue(1);
        assertEquals(2, v.getValue(), "addValue should increment unsigned int value by specified amount");
    }

    /**
     * Validates {@link ByteValue} operations for a heap instance.
     */
    @Test
    public void testHeapByteValue() {
        ByteValue byteValue = Values.newHeapInstance(ByteValue.class);
        assertNotNull(byteValue, "heap-allocated byte value instance should be created successfully");
        testByteValue(byteValue);
    }

    /**
     * Runs the same assertions against a native {@link ByteValue}.
     */
    @Test
    public void testNativeByteValue() {
        ByteValue byteValue = Values.newNativeReference(ByteValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) byteValue).maxSize());
        assertEquals(1, bs.capacity(), "native byte value should require 1-byte BytesStore capacity");
        ((Byteable) byteValue).bytesStore(bs, 0, ((Byteable) byteValue).maxSize());
        testByteValue(byteValue);
        bs.releaseLast();
    }

    /**
     * Core checks for {@link ByteValue} implementations.
     */
    private void testByteValue(ByteValue v) {
        assertEquals(0, v.getValue(), "byte value should initialize to zero on creation");

        v.setValue((byte) 1);
        assertEquals(1, v.getValue(), "setValue should update byte value to specified amount");

        v.addValue((byte) 1);
        assertEquals(2, v.getValue(), "addValue should increment byte value by specified amount");
    }

    /**
     * Checks that a heap-backed {@link CharValue} supports basic set and get.
     */
    @Test
    public void testHeapCharValue() {
        CharValue charValue = Values.newHeapInstance(CharValue.class);
        assertNotNull(charValue, "heap-allocated char value instance should be created successfully");
        testCharValue(charValue);
    }

    /**
     * Ensures the native version of {@link CharValue} behaves the same as the heap one.
     */
    @Test
    public void testNativeCharValue() {
        CharValue charValue = Values.newNativeReference(CharValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) charValue).maxSize());
        assertEquals(2, bs.capacity(), "native char value should require 2-byte BytesStore capacity");
        ((Byteable) charValue).bytesStore(bs, 0, ((Byteable) charValue).maxSize());
        testCharValue(charValue);
        bs.releaseLast();
    }

    /**
     * Helper for verifying {@link CharValue} semantics.
     */
    private void testCharValue(CharValue v) {
        assertEquals(0, v.getValue(), "char value should initialize to null character on creation");

        v.setValue((char) 1);
        assertEquals(1, v.getValue(), "setValue should update char value to specified character");
    }

    /**
     * Uses a heap-backed {@link LongValue} to exercise atomic operations.
     */
    @Test
    public void testHeapLongValue() {
        LongValue longValue = Values.newHeapInstance(LongValue.class);
        assertNotNull(longValue, "heap-allocated long value instance should be created successfully");
        testLongValue(longValue);
    }

    /**
     * Runs the same checks on a native {@link LongValue} instance.
     */
    @Test
    public void testNativeLongValue() {
        try (LongValue longValue = Values.newNativeReference(LongValue.class)) {
            Byteable longByteableValue = (Byteable) longValue;
            BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(longByteableValue.maxSize());
            assertEquals(8, bs.capacity(), "native long value should require 8-byte BytesStore capacity");
            longByteableValue.bytesStore(bs, 0, longByteableValue.maxSize());
            testLongValue(longValue);
            bs.releaseLast();
        }
    }

    /**
     * Common logic for both heap and native {@link LongValue} checks.
     */
    private void testLongValue(LongValue v) {
        assertEquals(0, v.getValue(), "long value should initialize to zero on creation");

        v.setValue(1L);
        assertEquals(1, v.getValue(), "setValue should update long value to specified amount");

        v.addValue(1);
        assertEquals(2, v.getValue(), "addValue should increment long value by specified amount");

        v.addAtomicValue(-1);
        assertEquals(1, v.getValue(), "addAtomicValue should atomically decrement long value");

        assertTrue(v.compareAndSwapValue(1, 2), "compareAndSwapValue should succeed when current value matches expected");
        assertEquals(2, v.getValue(), "compareAndSwapValue should apply new value when swap succeeds");
        assertFalse(v.compareAndSwapValue(1, 2), "compareAndSwapValue should fail when current value does not match expected");
        assertEquals(2, v.getValue(), "compareAndSwapValue should leave value unchanged when swap fails");

        v.setOrderedValue(3);
        assertEquals(3, v.getValue(), "setOrderedValue should update long value with memory ordering semantics");
    }
    // CPD-ON

    /**
     * Verifies {@link FloatValue} behaviour in heap form.
     */
    @Test
    public void testHeapFloatValue() {
        FloatValue floatValue = Values.newHeapInstance(FloatValue.class);
        assertNotNull(floatValue, "heap-allocated float value instance should be created successfully");
        testFloatValue(floatValue);
    }

    /**
     * Executes the same checks using a native {@link FloatValue}.
     */
    @Test
    public void testNativeFloatValue() {
        FloatValue floatValue = Values.newNativeReference(FloatValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) floatValue).maxSize());
        assertEquals(4, bs.capacity(), "native float value should require 4-byte BytesStore capacity");
        ((Byteable) floatValue).bytesStore(bs, 0, ((Byteable) floatValue).maxSize());
        testFloatValue(floatValue);
        bs.releaseLast();
    }

    /**
     * Assertions shared by heap and native {@link FloatValue} tests.
     */
    private void testFloatValue(FloatValue v) {
        assertEquals(0.0f, v.getValue(), 0.0f, "float value should initialize to zero on creation");

        v.setValue(1.0f);
        assertEquals(1.0f, v.getValue(), 0.0f, "setValue should update float value to specified amount");

        v.addValue(1.0f);
        assertEquals(1.0f + 1.0f, v.getValue(), 0.0f, "addValue should increment float value by specified amount");
        float v2 = v.getValue();

        v.addAtomicValue(-1.0f);
        assertEquals(v2 + (-1.0f), v.getValue(), 0.0f, "addAtomicValue should atomically decrement float value");

        v.setOrderedValue(3.0f);
        assertEquals(3.0f, v.getValue(), 0.0f, "setOrderedValue should update float value with memory ordering semantics");
    }

    /**
     * Validates {@link DoubleValue} when allocated on the heap.
     */
    @Test
    public void testHeapDoubleValue() {
        DoubleValue doubleValue = Values.newHeapInstance(DoubleValue.class);
        assertNotNull(doubleValue, "heap-allocated double value instance should be created successfully");
        testDoubleValue(doubleValue);
    }

    /**
     * Runs the same assertions on a native {@link DoubleValue} backed by a {@link BytesStore}.
     */
    @Test
    public void testNativeDoubleValue() {
        DoubleValue doubleValue = newBackedNativeDoubleValue();
        assertNotNull(doubleValue, "native-allocated double value instance should be created successfully");
        testDoubleValue(doubleValue);
        ((Byteable) doubleValue).bytesStore().releaseLast();
    }

    @NotNull
    private DoubleValue newBackedNativeDoubleValue() {
        DoubleValue doubleValue = Values.newNativeReference(DoubleValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) doubleValue).maxSize());
        assertEquals(8, bs.capacity(), "native double value should require 8-byte BytesStore capacity");
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
        assertEquals(nativeDoubleValue, heapDoubleValue, "native and heap double values should be equal when holding same data");
        ((Byteable) nativeDoubleValue).bytesStore().releaseLast();
    }

    /**
     * Common set of assertions for {@link DoubleValue} instances.
     */
    private void testDoubleValue(DoubleValue v) {
        assertEquals(0.0, v.getValue(), 0.0, "double value should initialize to zero on creation");

        v.setValue(1.0);
        assertEquals(1.0, v.getValue(), 0.0, "setValue should update double value to specified amount");

        v.addValue(1.0);
        assertEquals(1.0 + 1.0, v.getValue(), 0.0, "addValue should increment double value by specified amount");
        double v2 = v.getValue();

        v.addAtomicValue(-1.0);
        assertEquals(v2 + (-1.0), v.getValue(), 0.0, "addAtomicValue should atomically decrement double value");

        v.setOrderedValue(3.0);
        assertEquals(3.0, v.getValue(), 0.0, "setOrderedValue should update double value with memory ordering semantics");
    }

    /**
     * Checks a heap-backed {@link ShortValue} for simple arithmetic operations.
     */
    @Test
    public void testHeapShortValue() {
        ShortValue shortValue = Values.newHeapInstance(ShortValue.class);
        assertNotNull(shortValue, "heap-allocated short value instance should be created successfully");
        testShortValue(shortValue);
    }

    /**
     * Verifies that the native version of {@link ShortValue} matches the heap behaviour.
     */
    @Test
    public void testNativeShortValue() {
        ShortValue shortValue = Values.newNativeReference(ShortValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) shortValue).maxSize());
        assertEquals(2, bs.capacity(), "native short value should require 2-byte BytesStore capacity");
        ((Byteable) shortValue).bytesStore(bs, 0, ((Byteable) shortValue).maxSize());
        testShortValue(shortValue);
        bs.releaseLast();
    }

    /**
     * Helper for {@link ShortValue} arithmetic tests.
     */
    private void testShortValue(ShortValue v) {
        assertEquals(0, v.getValue(), "short value should initialize to zero on creation");

        v.setValue((short) 1);
        assertEquals(1, v.getValue(), "setValue should update short value to specified amount");

        v.addValue((short) 1);
        assertEquals(2, v.getValue(), "addValue should increment short value by specified amount");
    }

    /**
     * Checks a heap-backed {@link BooleanValue} toggles correctly.
     */
    @Test
    public void testHeapBooleanValue() {
        BooleanValue booleanValue = Values.newHeapInstance(BooleanValue.class);
        assertNotNull(booleanValue, "heap-allocated boolean value instance should be created successfully");
        testBooleanValue(booleanValue);
    }

    /**
     * Performs the same toggle test on a native {@link BooleanValue}.
     */
    @Test
    public void testNativeBooleanValue() {
        BooleanValue booleanValue = Values.newNativeReference(BooleanValue.class);
        BytesStore<?, ?> bs = nativeStoreWithFixedCapacity(((Byteable) booleanValue).maxSize());
        assertEquals(1, bs.capacity(), "native boolean value should require 1-byte BytesStore capacity");
        ((Byteable) booleanValue).bytesStore(bs, 0, ((Byteable) booleanValue).maxSize());
        testBooleanValue(booleanValue);
        bs.releaseLast();
    }

    /**
     * Helper used by both heap and native boolean tests.
     */
    private void testBooleanValue(BooleanValue v) {
        assertFalse(v.getValue(), "boolean value should initialize to false on creation");

        v.setValue(true);
        assertTrue(v.getValue(), "setValue should update boolean value to true");
    }
}
