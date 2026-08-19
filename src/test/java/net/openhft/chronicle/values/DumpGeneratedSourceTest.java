/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Regression test for issue #42: when generated-source dumping is enabled the
 * generated heap/native Java source is written to disk so it can be stepped
 * through in a debugger.
 */
public class DumpGeneratedSourceTest {

    private String savedDump;
    private String savedDir;

    @After
    public void restoreProperties() {
        restore(ValueModel.DUMP_CODE_PROPERTY, savedDump);
        restore(ValueModel.GENERATED_SOURCE_DIR_PROPERTY, savedDir);
    }

    private static void restore(String key, String value) {
        if (value == null)
            System.clearProperty(key);
        else
            System.setProperty(key, value);
    }

    @Test
    public void dumpingDisabledByDefault() {
        savedDump = System.getProperty(ValueModel.DUMP_CODE_PROPERTY);
        savedDir = System.getProperty(ValueModel.GENERATED_SOURCE_DIR_PROPERTY);
        System.clearProperty(ValueModel.DUMP_CODE_PROPERTY);
        // Jvm.isDebug() is false under a normal test run, so no directory is used.
        assertNull(ValueModel.generatedSourceDir());
    }

    @Test
    public void generatedSourceDirHonoursConfiguredDirectory() {
        savedDump = System.getProperty(ValueModel.DUMP_CODE_PROPERTY);
        savedDir = System.getProperty(ValueModel.GENERATED_SOURCE_DIR_PROPERTY);
        System.setProperty(ValueModel.DUMP_CODE_PROPERTY, "true");
        System.setProperty(ValueModel.GENERATED_SOURCE_DIR_PROPERTY, "/tmp/some-dump-dir");
        assertEquals(new File("/tmp/some-dump-dir"), ValueModel.generatedSourceDir());
    }

    @Test
    public void generatedSourceIsWrittenToDiskWhenDumping() throws IOException {
        savedDump = System.getProperty(ValueModel.DUMP_CODE_PROPERTY);
        savedDir = System.getProperty(ValueModel.GENERATED_SOURCE_DIR_PROPERTY);

        final Path dumpDir = Files.createTempDirectory("chronicle-values-dump");
        System.setProperty(ValueModel.DUMP_CODE_PROPERTY, "true");
        System.setProperty(ValueModel.GENERATED_SOURCE_DIR_PROPERTY, dumpDir.toString());

        // First use of DumpTestValue in this JVM: forces fresh generation via the
        // dump-configured compiler, which writes the source to disk.
        final DumpTestValue value = Values.newHeapInstance(DumpTestValue.class);
        value.num(42);
        value.size(1024L);
        assertEquals(42, value.num());
        assertEquals(1024L, value.size());

        try (Stream<Path> paths = Files.walk(dumpDir)) {
            final List<Path> javaFiles = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            assertFalse("expected generated .java source under " + dumpDir + " but found none",
                    javaFiles.isEmpty());
            final boolean forOurType = javaFiles.stream()
                    .anyMatch(p -> p.getFileName().toString().contains("DumpTestValue"));
            assertTrue("expected a dumped source file for DumpTestValue, got " + javaFiles, forOurType);
        }
    }
}
