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

import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Interface holding a five element array of {@code long} values for field type tests.
 */
interface FiveLongValues {
    /**
     * Stores the supplied value at the given index.
     *
     * @param i position in the five element array
     * @param v value to store
     */
    @Array(length = 5)
    void setValueAt(int i, long v);

    /**
     * Returns the value at the given index.
     *
     * @param i position in the five element array
     * @return stored value
     */
    long getValueAt(int i);
}

/**
 * Interface holding a five element array of booleans for field type tests.
 */
interface FiveBooleanValues {
    /**
     * Stores the supplied flag at the given index.
     *
     * @param i position in the five element array
     * @param v flag to store
     */
    @Array(length = 5)
    void setValueAt(int i, boolean v);

    /**
     * Returns the flag at the given index.
     *
     * @param i position in the five element array
     * @return stored flag
     */
    boolean getValueAt(int i);
}

/**
 * Combines long and boolean arrays so that both primitive types appear in the
 * generated proxy.
 */
interface FiveLongAndBooleanValues {
    /** Returns the long array view. */
    FiveLongValues getLongValues();

    /** Assigns the long array view. */
    void setLongValues(FiveLongValues values);

    /** Returns the boolean array view. */
    FiveBooleanValues getBooleanValues();

    /** Assigns the boolean array view. */
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
