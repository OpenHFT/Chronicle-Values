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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import net.openhft.chronicle.core.threads.CleaningThread;
import net.openhft.chronicle.core.threads.ThreadDump;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import org.junit.After;
import org.junit.Before;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Base class for values tests.
 *
 * Enables reference tracing and starts exception recording before each test.
 * At the end of a test it verifies that all resources have been released,
 * no unexpected threads remain and only declared exceptions were logged.
 */

public class ValuesTestCommon {

    protected ThreadDump threadDump;
    protected Map<ExceptionKey, Integer> exceptions;
    private final Map<Predicate<ExceptionKey>, String> expectedExceptions = new LinkedHashMap<>();

    @Before
    public void enableReferenceTracing() {
        AbstractReferenceCounted.enableReferenceTracing();
    }

    /**
     * Fails the test if any {@code AbstractReferenceCounted} instances have not
     * reached a reference count of zero.
     */
    public void assertReferencesReleased() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @Before
    public void recordExceptions() {
        exceptions = Jvm.recordExceptions();
    }

    /**
     * Registers an expected log entry containing the supplied message.
     */
    public void expectException(String message) {
        expectException(k -> k.message.contains(message) ||
                (k.throwable != null &&
                        k.throwable.getMessage().contains(message)), message);
    }

    /**
     * Registers an expected log entry that matches the given predicate.
     *
     * @param predicate test for matching exception keys
     * @param description text used if the expected entry is missing
     */
    public void expectException(Predicate<ExceptionKey> predicate, String description) {
        expectedExceptions.put(predicate, description);
    }

    /**
     * Verifies that only declared exceptions were recorded during the test.
     * Any unexpected entry causes the test to fail after dumping the log.
     */
    public void checkExceptions() {
        for (Map.Entry<Predicate<ExceptionKey>, String> expectedException :
                expectedExceptions.entrySet()) {
            if (!exceptions.keySet().removeIf(expectedException.getKey())) {
                Slf4jExceptionHandler.WARN.on(getClass(),
                        "No error for " + expectedException.getValue());
            }
        }
        expectedExceptions.clear();
        if (Jvm.hasException(exceptions)) {
            Jvm.dumpException(exceptions);
            Jvm.resetExceptionHandlers();
            throw new AssertionError(exceptions.keySet());
        }
    }

    @After
    public void afterChecks() {
        SystemTimeProvider.CLOCK = SystemTimeProvider.INSTANCE;
        CleaningThread.performCleanup(Thread.currentThread());

        // find any discarded resources.
        System.gc();
        AbstractCloseable.waitForCloseablesToClose(100);

        assertReferencesReleased();
        checkThreadDump();
        checkExceptions();
    }
}
