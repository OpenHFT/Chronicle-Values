/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UnderscoreFieldNameTest extends ValuesTestCommon {

    @Test
    public void testUnderscoreFieldName() {
        assertNotNull(Values.heapClassFor(UnderscoreFieldNameInterface.class), "underscore: heap class generated");
        assertNotNull(Values.nativeClassFor(UnderscoreFieldNameInterface.class), "underscore: native class generated");
    }
}
