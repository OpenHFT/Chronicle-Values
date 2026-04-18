/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * Example interface used by the tests to exercise generation of nested arrays.
 * <p>
 * The {@code text} field is followed by two arrays: one of primitive ints and
 * one of {@link JavaBeanInterface} values. The {@link MaxUtf8Length} annotation
 * on {@link #setIntAt(int, int)} is deliberately superfluous; it normally
 * applies to character data and is included only to ensure such annotations are
 * ignored for primitive fields.
 */
public interface NestedArrayInterface {

    /** Returns the free-form text associated with this value. */
    String getText();

    /** Updates the text field. */
    void setText(String text);

    /**
     * Retrieves the integer stored at {@code index}.
     * The valid index range is defined by the test that uses this interface.
     */
    int getIntAt(int index);

    /**
     * Stores {@code value} at {@code index}. The {@code @MaxUtf8Length(16)}
     * annotation is ignored as the parameter is not character data.
     */
    void setIntAt(@MaxUtf8Length(16) int index, int value);

    /**
     * Returns the nested bean at {@code index}. The array contains
     * 32 elements as declared by {@code @Array(length = 32)}.
     */
    @Array(length = 32)
    JavaBeanInterface getJBIAt(int index);

    /**
     * Updates the nested bean stored at {@code index}.
     */
    void setJBIAt(int index, JavaBeanInterface jbi);
}
