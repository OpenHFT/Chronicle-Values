/*
 * Copyright 2016-2025 chronicle.software
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
