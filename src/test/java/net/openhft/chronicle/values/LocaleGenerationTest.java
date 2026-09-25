/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocaleGenerationTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void turkishStartupGeneratesUsableHeapAndNativeValues() throws Exception {
        runChild("tr", "TR");
    }

    @Test
    public void englishStartupGeneratesUsableHeapAndNativeValues() throws Exception {
        runChild("en", "GB");
    }

    private void runChild(String language, String country) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(new File(System.getProperty("java.home"), "bin/java").getPath());
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments())
            if (argument.startsWith("--add-exports=") || argument.startsWith("--add-opens="))
                command.add(argument);
        command.add("-Duser.language=" + language);
        command.add("-Duser.country=" + country);
        command.add("-Dfile.encoding=UTF-8");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(LocaleGenerationProbe.class.getName());
        command.add(language);
        File output = temporary.newFile(language + ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output).start();
        Throwable primary = null;
        try {
            assertTrue("Locale child exceeded its 30-second fixture bound", process.waitFor(30, TimeUnit.SECONDS));
            String text = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
            assertEquals(text, 0, process.exitValue());
            assertTrue(text, text.contains("heap=PASS") && text.contains("native=PASS"));
        } catch (Exception | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            if (process.isAlive()) {
                try {
                    process.destroyForcibly();
                    assertTrue("Locale child did not terminate", process.waitFor(5, TimeUnit.SECONDS));
                } catch (Exception | Error cleanup) {
                    if (primary != null)
                        primary.addSuppressed(cleanup);
                    else
                        throw cleanup;
                }
            }
        }
    }
}
