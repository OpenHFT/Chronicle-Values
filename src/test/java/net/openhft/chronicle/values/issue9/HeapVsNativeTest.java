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

package net.openhft.chronicle.values.issue9;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.values.MaxUtf8Length;
import net.openhft.chronicle.values.NotNull;
import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that heap and native {@link Entity} values hold the same content but
 * do not return the original {@link String} instance when read back.
 */
public class HeapVsNativeTest extends ValuesTestCommon {
    public static final String SYMBOL = "symbol";

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
        assertTrue(SYMBOL.contentEquals(entity.getSymbol()));
        assertNotEquals(SYMBOL, entity.getSymbol());
    }

    public interface Entity {
        CharSequence getSymbol();

        void setSymbol(@NotNull @MaxUtf8Length(6) CharSequence symbol);
    }
}
