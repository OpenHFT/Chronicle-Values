/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import org.junit.Test;

public class UnderscoreFieldNameTest extends ValuesTestCommon {

    @Test
    public void testUnderscoreFieldName() {
        Values.heapClassFor(UnderscoreFieldNameInterface.class);
        Values.nativeClassFor(UnderscoreFieldNameInterface.class);
    }
}
