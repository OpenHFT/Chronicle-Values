/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.issue10;

import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.junit.Test;

public class ChronicleValueTypeTest extends ValuesTestCommon {

    @Test
    public void testChronicleValueDate() {
        Values.heapClassFor(ChronicleValueDate.class);
        Values.nativeClassFor(ChronicleValueDate.class);
    }
}
