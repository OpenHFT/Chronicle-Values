/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * First level nested value containing a text {@code key} and two
 * {@link NestedB} children named {@code one} and {@code two}.
 *
 * <p>Setter parameter names are:</p>
 * <ul>
 * <li>{@code key(String key)}</li>
 * <li>{@code one(NestedB one)}</li>
 * <li>{@code two(NestedB one)}</li>
 * </ul>
 */
public interface NestedA {
    void key(@MaxUtf8Length(64) String key);

    String key();

    void one(NestedB one);

    NestedB one();

    void two(NestedB one);

    NestedB two();
}
