/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EnumsTest {

    private enum SampleStatus {
        NEW,
        ACTIVE,
        CLOSED
    }

    @Test
    public void findsEnumUniverse() {
        SampleStatus[] universe = Enums.getUniverse(SampleStatus.class);
        assertArrayEquals(SampleStatus.values(), universe);
        assertEquals(SampleStatus.values().length, Enums.numberOfConstants(SampleStatus.class));
    }
}
