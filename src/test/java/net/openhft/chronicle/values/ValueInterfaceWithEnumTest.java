/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.values.ValueInterfaceWithEnumTest.SimpleValueInterface.SVIEnum.SIX;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ges
 * @since 3/2/16.
 */
public class ValueInterfaceWithEnumTest extends ValuesTestCommon {

    /**
     * This test will throw an {@link ArrayIndexOutOfBoundsException}. This seems to occur only with Enums having even number of
     * values
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testValueInterface() {
        SimpleValueInterface nativeValue = Values.newNativeReference(SimpleValueInterface.class);
        int modelSize = ValueModel.acquire(SimpleValueInterface.class).sizeInBytes();
        ((Byteable) nativeValue).bytesStore(BytesStore.wrap(new byte[modelSize]), 0, modelSize);
        SimpleValueInterface heapValue = Values.newHeapInstance(SimpleValueInterface.class);

        nativeValue.setId(1);
        nativeValue.setTruth(true);
        nativeValue.setSVIEnum(SIX);

        heapValue.copyFrom(nativeValue);

        assertEquals(1, heapValue.getId(), "heapValue: id copied from native");
        assertTrue(heapValue.getTruth(), "heapValue: truth copied from native");
        assertEquals(SIX, heapValue.getSVIEnum(), "heapValue: enum copied from native");

        heapValue.setId(2);
        heapValue.setTruth(false);
        heapValue.setSVIEnum(null);

        nativeValue.copyFrom(heapValue);

        assertEquals(2, nativeValue.getId(), "nativeValue: id copied from heap");
        assertFalse(nativeValue.getTruth(), "nativeValue: truth copied from heap");
        assertNull(nativeValue.getSVIEnum(), "nativeValue: enum copied from heap");
    }

    public interface SimpleValueInterface extends Copyable<SimpleValueInterface> {
        int getId();

        void setId(int id);

        boolean getTruth();

        void setTruth(boolean truth);

        SVIEnum getSVIEnum();

        void setSVIEnum(SVIEnum val);

        enum SVIEnum {
            ONE, TWO, THREE, FOUR, FIVE, SIX
        }
    }
}
