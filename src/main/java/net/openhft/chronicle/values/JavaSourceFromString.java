//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

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

import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * Presents a snippet of Java source held in memory as a file object.
 * The compiler can therefore treat dynamically generated code as if it
 * were loaded from the file system.
 */
class JavaSourceFromString extends SimpleJavaFileObject {
    /**
     * The Java source that backs this pseudo file.
     */
    private final String code;

    /**
     * Creates a new instance for the given compilation unit name.
     *
     * @param name logical name of the unit, used when forming the URI
     * @param code source code for the unit
     */
    JavaSourceFromString(@NotNull String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                Kind.SOURCE);
        this.code = code;
    }

    /**
     * Returns the stored source code.
     * The {@code ignoreEncodingErrors} flag is ignored as the code is already a
     * {@link String}.
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
