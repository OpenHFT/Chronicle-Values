/*
 * Copyright 2016-2021 chronicle.software
 *
 * https://chronicle.software
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
    @Test
    public void testValueInterface() {
        SimpleValueInterface nativeValue = Values.newNativeReference(SimpleValueInterface.class);
        int modelSize = ValueModel.acquire(SimpleValueInterface.class).sizeInBytes();
        //noinspection unchecked
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
