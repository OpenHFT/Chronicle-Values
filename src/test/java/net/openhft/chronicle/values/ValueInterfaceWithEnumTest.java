/*
 *      Copyright (C) 2015  higherfrequencytrading.com
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
public class ValueInterfaceWithEnumTest {

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
        enum SVIEnum {
            ONE, TWO, THREE, FOUR, FIVE, SIX
        }

        int getId();
        void setId(int id);
        boolean getTruth();
        void setTruth(boolean truth);

        SVIEnum getSVIEnum();
        void setSVIEnum(SVIEnum val);
    }
}
