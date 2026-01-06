/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.openhft.chronicle.values.Values.newHeapInstance;
import static net.openhft.chronicle.values.Values.newNativeReference;
import static org.junit.jupiter.api.Assertions.*;

public class ComplexValueTest extends ValuesTestCommon {

    @Test
    public void heapAndNativeBehaviourMatch() {
        ComplexValue heap = newHeapInstance(ComplexValue.class);
        ComplexValue nativeValue = newNativeReference(ComplexValue.class);
        BytesStore<?, ?> store =
                BytesStore.nativeStoreWithFixedCapacity(nativeValue.maxSize());
        try {
            nativeValue.bytesStore(store, 0, nativeValue.maxSize());
            mutateComplexValue(heap);
            mutateComplexValue(nativeValue);

            // copy and equality
            heap.copyFrom(nativeValue);
            assertEquals(nativeValue, heap, "copyFrom: heap equals native");
            assertEquals(nativeValue.hashCode(), heap.hashCode(), "copyFrom: hashCode matches");

            ComplexValue clone = newHeapInstance(ComplexValue.class);
            clone.copyFrom(heap);
            assertEquals(heap, clone, "copyFrom: clone equals heap");

            // layout inspection exercises ValueModel metadata paths
            ValueModel model = ValueModel.acquire(ComplexValue.class);
            assertNotNull(model.nativeClass(), "value model: native class generated");
            assertNotNull(model.heapClass(), "value model: heap class generated");
            Map<String, FieldModel> byName = model.fields()
                    .collect(Collectors.toMap(FieldModel::name, Function.identity()));

            assertTrue(byName.containsKey("label"), "label field present");
            assertTrue(byName.containsKey("mirrorEnabled"), "mirrorEnabled field present");
            assertTrue(model.fieldBitOffset(byName.get("label")) >= 0, "label offset should be resolved");
            assertTrue(model.fieldBitExtent(byName.get("history")) >= 3 * Long.SIZE,
                    "history should allocate extent per element");

            // behaviour assertions
            assertEquals(ComplexValue.Status.ACTIVE, nativeValue.getStatus(), "native: status");
            assertEquals("Chronicle", nativeValue.getLabel(), "native: label");
            assertEquals(42L, nativeValue.getHistoryAt(0), "native: history[0]");
            assertEquals(43L, nativeValue.getHistoryAt(1), "native: history[1]");
            assertEquals(44L, nativeValue.getHistoryAt(2), "native: history[2]");
            assertEquals(35.0f, nativeValue.getBalance(), 0.0f, "native: balance");
            assertTrue(model.recommendedOffsetAlignment() >= 1, "value model: recommended alignment is positive");
        } finally {
            store.releaseLast();
        }
    }

    @Test
    public void labelRespectsUtf8Limit() {
        ComplexValue value = newHeapInstance(ComplexValue.class);
        value.setLabel("This label is definitely beyond twelve chars");
        assertEquals("This label is definitely beyond twelve chars", value.getLabel(), "label: roundtrip");
    }

    private static void mutateComplexValue(ComplexValue value) {
        value.setCode((byte) 7);
        value.setEnabled(true);
        value.setMirrorEnabled(false);
        value.setId(101L);
        value.setStatus(ComplexValue.Status.ACTIVE);
        value.setLabel("Chronicle");
        for (int i = 0; i < 3; i++) {
            value.setHistoryAt(i, 42L + i);
        }
        value.setBalance(10.0f);
        value.setOrderedBalance(20.0f);
        assertTrue(value.compareAndSwapBalance(20.0f, 30.0f), "CAS: expected success for matching balance");
        assertFalse(value.compareAndSwapBalance(20.0f, 40.0f), "CAS: expected failure for mismatched balance");
        assertEquals(35.0f, value.addBalance(5.0f), 0.0f, "balance: addBalance result");
    }
}
