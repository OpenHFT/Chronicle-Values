/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.issue10;

/**
 * Test interface for Issue 10.
 *
 * <p>Extends {@link ChronicleValueType} so the tests can verify that heap and
 * native implementations are generated correctly.</p>
 */
interface ChronicleValueDate extends ChronicleValueType<ChronicleValueDate> {
}
