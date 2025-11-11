/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.core.values.LongValue;

/**
 * Interface exercising {@code getUsing} operations on a two element array of
 * {@link LongValue} instances.
 */
public interface JavaBeanInterfaceGetUsingAt {

    /**
     * Writes {@code using} into the slot identified by {@code index}.
     *
     * @param index zero-based array index, less than two
     * @param using value to be copied into the array
     */
    @Array(length = 2)
    void setItemAt(int index, LongValue using);

    /**
     * Copies the value stored at the given index into {@code using} to avoid
     * object creation.
     *
     * @param index zero-based array index, less than two
     * @param using container receiving the value
     * @return the passed in container for chaining
     */
    LongValue getUsingItemAt(int index, LongValue using);
}
