/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.issue10;

import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ChronicleValueTypeTest extends ValuesTestCommon {

    @Test
    public void testChronicleValueDate() {
        assertNotNull(Values.heapClassFor(ChronicleValueDate.class), "chronicle value date: heap class generated");
        assertNotNull(Values.nativeClassFor(ChronicleValueDate.class), "chronicle value date: native class generated");
    }
}
