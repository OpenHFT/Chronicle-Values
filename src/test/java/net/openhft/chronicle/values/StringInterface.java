/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * Used for basic string handling tests.
 */
public interface StringInterface {
    String getString();

    void setString(@MaxUtf8Length(64) String s);

    String getText();

    void setText(@MaxUtf8Length(64) String s);
}
