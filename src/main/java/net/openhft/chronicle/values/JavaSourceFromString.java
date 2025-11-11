/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
