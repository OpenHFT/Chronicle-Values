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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.junit.Assert.assertTrue;

public class SimpleURIClassObjectTest {

    @Test
    public void supportsJrtSchemes() throws Exception {
        URI jrtUri = Object.class.getResource("Object.class").toURI();
        SimpleURIClassObject object = new SimpleURIClassObject(jrtUri, Object.class);
        try (InputStream in = object.openInputStream()) {
            assertTrue("Expected content from jrt URI", in.read() >= 0);
        }
    }

    @Test(expected = IOException.class)
    public void rejectsUnknownSchemes() throws IOException {
        SimpleURIClassObject object =
                new SimpleURIClassObject(URI.create("mailto:test@example.invalid"), Object.class);
        object.openInputStream();
    }
}
