/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import org.junit.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class AnnotationFileManagerTest {
    @Test
    public void annotationsRemainReadableWithoutACompilerClasspath() throws Exception {
        try (StandardJavaFileManager delegate = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {
            delegate.setLocation(StandardLocation.CLASS_PATH, Collections.emptyList());
            try (MyJavaFileManager manager = new MyJavaFileManager(Value.class, delegate)) {
                Set<String> names = new HashSet<>();
                for (JavaFileObject file : manager.list(StandardLocation.CLASS_PATH,
                        "org.jetbrains.annotations", Collections.singleton(JavaFileObject.Kind.CLASS), false)) {
                    names.add(manager.inferBinaryName(StandardLocation.CLASS_PATH, file));
                    try (DataInputStream input = new DataInputStream(file.openInputStream())) {
                        assertEquals("Preloaded annotation must expose its actual class bytes", 0xCAFEBABE, input.readInt());
                    }
                }
                assertEquals(new HashSet<>(Arrays.asList("org.jetbrains.annotations.NotNull",
                        "org.jetbrains.annotations.Nullable")), names);
            }
        }
    }

    public interface Value {
        int getId();
        void setId(int id);
    }
}
