/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * Test bean exposing several enums to ensure code generation copes with multiple
 * enumeration fields. Two members use {@link MyEnum} and another holds a
 * {@link BuySell} flag.
 */
public interface JavaBeanInterfaceMoreThanOneEnums {

    /**
     * Returns the first test enum.
     *
     * @return assigned constant
     */
    MyEnum getMyEnum1();

    /**
     * Updates the first test enum field.
     *
     * @param myEnum value to store
     */
    void setMyEnum1(MyEnum myEnum);

    /**
     * Returns the second test enum.
     *
     * @return assigned constant
     */
    MyEnum getMyEnum2();

    /**
     * Updates the second test enum field.
     *
     * @param myEnum value to store
     */
    void setMyEnum2(MyEnum myEnum);

    /**
     * Returns the trade side flag.
     *
     * @return BUY or SELL
     */
    BuySell getBuySell();

    /**
     * Updates the trade side flag.
     *
     * @param myEnum BUY or SELL value
     */
    void setBuySell(BuySell myEnum);
}
