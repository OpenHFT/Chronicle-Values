/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;

/**
 * Default {@link Byteable} behaviour for heap-backed values. Each method
 * throws {@link UnsupportedOperationException} because heap instances are not
 * associated with a {@code BytesStore}.
 */
@SuppressWarnings("rawtypes")
public interface HeapByteable extends Byteable {

    /**
     * Heap objects cannot be bound to a {@link BytesStore}; this method always
     * throws.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    default void bytesStore(BytesStore bytesStore, long l, long l1) {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    /**
     * Heap objects do not have a backing store; this method always throws.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    default BytesStore<?, ?> bytesStore() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    /**
     * Heap values are not memory-mapped so an offset cannot be returned.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    default long offset() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    /**
     * Heap values have no maximum serialised size; this method always throws.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    default long maxSize() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }
}
