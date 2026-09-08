/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.filemanager;

public interface BlockingValue {
    long getValue();

    void setValue(long value);

    interface OtherValue {
        int getValue();

        void setValue(int value);
    }
}
