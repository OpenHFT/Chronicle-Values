/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.values;

import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

interface FiveLongValues {
    @Array(length = 5)
    void setValueAt(int i, long v);

    long getValueAt(int i);
}

interface FiveBooleanValues {
    @Array(length = 5)
    void setValueAt(int i, boolean v);

    boolean getValueAt(int i);
}

interface FiveLongAndBooleanValues {
    FiveLongValues getLongValues();

    void setLongValues(FiveLongValues values);

    FiveBooleanValues getBooleanValues();

    void setBooleanValues(FiveBooleanValues values);
}

public class FirstPrimitiveFieldTest extends ValuesTestCommon {

    @Test
    public void firstPrimitiveFieldTest() {
        assertEquals(int.class, ValueModel.acquire(IntValue.class).firstPrimitiveFieldType());
        assertEquals(long.class, ValueModel.acquire(LongValue.class).firstPrimitiveFieldType());
        assertEquals(long.class,
                ValueModel.acquire(Values.nativeClassFor(LongValue.class))
                        .firstPrimitiveFieldType());
        assertEquals(long.class,
                ValueModel.acquire(FiveLongValues.class).firstPrimitiveFieldType());
        assertEquals(boolean.class,
                ValueModel.acquire(FiveBooleanValues.class).firstPrimitiveFieldType());
        assertEquals(long.class,
                ValueModel.acquire(FiveLongAndBooleanValues.class).firstPrimitiveFieldType());
    }
}
