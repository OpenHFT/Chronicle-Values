/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import static net.openhft.chronicle.values.ValueInterfaceWithEnumTest.SimpleValueInterface.SVIEnum.SIX;
import static org.junit.Assert.assertEquals;

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

        assertEquals(1, heapValue.getId());
        assertEquals(true, heapValue.getTruth());
        assertEquals(SIX, heapValue.getSVIEnum());

        heapValue.setId(2);
        heapValue.setTruth(false);
        heapValue.setSVIEnum(null);

        nativeValue.copyFrom(heapValue);

        assertEquals(2, nativeValue.getId());
        assertEquals(false, nativeValue.getTruth());
        assertEquals(null, nativeValue.getSVIEnum());
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
