/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.issue9;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.values.MaxUtf8Length;
import net.openhft.chronicle.values.NotNull;
import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that heap and native {@link Entity} values hold the same content but
 * do not return the original {@link String} instance when read back.
 */
public class HeapVsNativeTest extends ValuesTestCommon {
    private static final String SYMBOL = "symbol";

    /**
     * Verifies the behaviour of a heap-backed {@link Entity} instance.
     */
    @Test
    public void heap() {
        Entity entity = Values.newHeapInstance(Entity.class);
        check(entity);
    }

    /**
     * Exercises a native reference and checks it behaves like the heap variant.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void nativeRef() {
        Entity entity = Values.newNativeReference(Entity.class);
        byte[] bytes = new byte[7];
        BytesStore<?, ?> bs = BytesStore.wrap(bytes);
        Byteable byteable = (Byteable) entity;
        byteable.bytesStore(bs, 0, bytes.length);
        check(entity);
    }

    /**
     * Common test logic that sets the symbol and ensures the stored sequence
     * is equal in content but not identical to the input String.
     */
    private void check(Entity entity) {
        entity.setSymbol(SYMBOL);
        assertTrue(SYMBOL.contentEquals(entity.getSymbol()), "entity: symbol content matches");
        assertNotEquals(SYMBOL, entity.getSymbol(), "entity: symbol is not the original String instance");
    }

    public interface Entity {
        CharSequence getSymbol();

        void setSymbol(@NotNull @MaxUtf8Length(6) CharSequence symbol);
    }
}
