/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
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
 * NOT FOR DIRECT USE. Implements {@link Byteable} by throwing {@link UnsupportedOperationException}
 * from all methods.
 */
public interface HeapByteable extends Byteable {

    @Override
    default void bytesStore(BytesStore bytesStore, long l, long l1) {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    @Override
    default BytesStore bytesStore() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    @Override
    default long offset() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    @Override
    default long maxSize() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }
}
