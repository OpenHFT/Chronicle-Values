/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * Second level nested interface used in tests.
 * <p>
 * Holds {@code bid} and {@code ask} values which act as simple price fields.
 * They are written to and read from nested structures so tests can verify
 * floating point values are preserved when the objects are encoded.
 */
public interface NestedB {
    void bid(double bid);

    double bid();

    void ask(double ask);

    double ask();
}
