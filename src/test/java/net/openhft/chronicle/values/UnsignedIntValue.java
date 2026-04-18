/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

public interface UnsignedIntValue {
    long getValue();

    void setValue(@Range(min = 0, max = (1L << 32) - 1) long value);

    long addValue(long addition);
}
