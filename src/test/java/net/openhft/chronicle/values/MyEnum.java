//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

package net.openhft.chronicle.values;

/**
 * Enumeration with numeric codes for verifying enum handling in unit tests.
 */
public enum MyEnum {
    /**
     * Constant {@code A} mapped to {@code 1}.
     */
    A(1),
    /**
     * Constant {@code B} mapped to {@code 2}.
     */
    B(2),
    /**
     * Constant {@code C} mapped to {@code 3}.
     */
    C(3);

    private final int var;

    MyEnum(int var) {
        this.var = var;
    }

    /**
     * Returns the numeric value associated with this constant. The tests
     * use this value as a custom encoding.
     */
    public int getVar() {
        return this.var;
    }
}
