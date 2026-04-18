/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.issue10;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.values.Copyable;

/**
 * Generic representation of a test value type.
 *
 * <p>The single {@code int} field is accessed via {@link #getValue()} and
 * {@link #setValue(int)}.  Implementations overlay a {@link net.openhft.chronicle.bytes.BytesStore}
 * so the value can be read and written directly to memory.  Instances can be
 * copied and marshalled using the super-interfaces.</p>
 */
@SuppressWarnings("rawtypes")
public interface ChronicleValueType<C extends ChronicleValueType<C>>
        extends Byteable, BytesMarshallable, Copyable<C> {

    /**
     * Reads the raw integer from the backing bytes.
     *
     * @return the current stored value
     */
    int getValue();

    /**
     * Writes the supplied integer to the backing bytes.  No validation is performed.
     *
     * @param value new value to store
     */
    void setValue(int value);
}
