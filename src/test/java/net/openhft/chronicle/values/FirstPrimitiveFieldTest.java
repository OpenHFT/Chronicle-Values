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
