/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.core.values.LongValue;
/**
 * Minimal bean exposing a two element array of {@link LongValue}.
 * Each element is accessed as a flyweight for test purposes.
 */
public interface JavaBeanInterfaceGetAt {

    /**
     * Copies the {@code using} value into the element at {@code index}.
     * Valid indexes are {@code 0} and {@code 1}.
     *
     * @param index position of the element to update
     * @param using flyweight supplying the value to copy
     */
    @Array(length = 2)
    void setItemAt(int index, LongValue using);

    /**
     * Returns a flyweight view of the element at {@code index}.
     * The returned instance may be reused between calls.
     * Valid indexes are {@code 0} and {@code 1}.
     *
     * @param index position of the element to read
     * @return flyweight representing the requested element
     */
    LongValue getItemAt(int index);
}
