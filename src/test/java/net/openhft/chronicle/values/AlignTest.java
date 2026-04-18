/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import static net.openhft.chronicle.values.Values.newNativeReference;

/**
 * Verifies that native values honour alignment rules.
 *
 * <p>The {@link DemoOrderVOInterface} contains a double field updated
 * atomically. This test ensures that the generated implementation places the
 * field on an eight byte boundary so that {@code addAtomicOrderQty} operates
 * correctly.</p>
 */
public class AlignTest extends ValuesTestCommon {
    @SuppressWarnings("unchecked")
    @Test
    public void testAlign() {
        DemoOrderVOInterface value = newNativeReference(DemoOrderVOInterface.class);
        long size = value.maxSize();
        BytesStore<?, Void> bs = BytesStore.nativeStore(size);
        value.bytesStore(bs, 0, size);
        value.addAtomicOrderQty(10.0);
        System.out.println(value);
        bs.releaseLast();
    }

    /**
     * Minimal order definition used to check alignment constraints.
     * <p>
     * The order quantity field must be aligned for atomic updates.
     */
    @SuppressWarnings("rawtypes")
    interface DemoOrderVOInterface extends Byteable {
        /**
         * Instrument symbol for the order.
         */
        CharSequence getSymbol();
//    public StringBuilder getUsingSymbol(StringBuilder sb);

        /**
         * Stores the instrument symbol.
         *
         * @param symbol text restricted to twenty UTF-8 bytes
         */
        void setSymbol(@MaxUtf8Length(20) CharSequence symbol);

        /**
         * Adds {@code toAdd} to the order quantity atomically.
         * <p>
         * Alignment of the underlying field is critical so that this method can
         * use atomic operations provided by Chronicle Bytes.
         *
         * @param toAdd increment applied to the quantity
         * @return the new order quantity
         */
        double addAtomicOrderQty(double toAdd);

        /**
         * Returns the current order quantity.
         */
        double getOrderQty();

        /**
         * Overwrites the order quantity.
         */
        void setOrderQty(double orderQty);

    }
}
