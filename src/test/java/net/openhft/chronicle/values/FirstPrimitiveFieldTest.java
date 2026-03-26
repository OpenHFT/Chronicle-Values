/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    /**
     * Returns the long array view.
     */
    FiveLongValues getLongValues();

    /**
     * Assigns the long array view.
     */
    void setLongValues(FiveLongValues values);

    /**
     * Returns the boolean array view.
     */
    FiveBooleanValues getBooleanValues();

    /**
     * Assigns the boolean array view.
     */
    void setBooleanValues(FiveBooleanValues values);
}

/**
 * Checks the primitive type discovered first by {@link ValueModel} for simple
 * values and for array-based views.
 */
public class FirstPrimitiveFieldTest extends ValuesTestCommon {

    @Test
    public void firstPrimitiveFieldTest() {
        assertSame(int.class, ValueModel.acquire(IntValue.class).firstPrimitiveFieldType());
        assertSame(long.class, ValueModel.acquire(LongValue.class).firstPrimitiveFieldType());
        assertSame(long.class, ValueModel.acquire(Values.nativeClassFor(LongValue.class))
                .firstPrimitiveFieldType());
        assertSame(long.class, ValueModel.acquire(FiveLongValues.class).firstPrimitiveFieldType());
        assertSame(boolean.class, ValueModel.acquire(FiveBooleanValues.class).firstPrimitiveFieldType());
        assertSame(long.class, ValueModel.acquire(FiveLongAndBooleanValues.class).firstPrimitiveFieldType());
    }
}
