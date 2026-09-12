/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.values.filemanager.BlockingValue;
import org.junit.Test;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class MyJavaFileManagerTest {

    @Test
    public void retainsConcurrentUpdatesToDifferentPackages() throws Exception {
        assertConcurrentUpdatesRetained(LongValue.class);
    }

    @Test
    public void retainsConcurrentUpdatesToTheSamePackage() throws Exception {
        assertConcurrentUpdatesRetained(BlockingValue.OtherValue.class);
    }

    private void assertConcurrentUpdatesRetained(Class<?> otherType) throws Exception {
        BlockingLoader loader = new BlockingLoader();
        Class<?> blockedType = loader.defineValue();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try (StandardJavaFileManager standard = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null)) {
            // The delegate must not supply the classes whose cache registration is under test.
            standard.setLocation(StandardLocation.CLASS_PATH, Collections.emptyList());
            MyJavaFileManager manager = new MyJavaFileManager(MyJavaFileManagerTest.class, standard);
            Future<?> first = executor.submit(() -> manager.addClassToFileObjects(blockedType));
            try {
                assertTrue("First registration did not reach class-resource lookup",
                        loader.resolving.await(10, TimeUnit.SECONDS));
                Future<?> second = executor.submit(() -> manager.addClassToFileObjects(otherType));
                second.get(10, TimeUnit.SECONDS);

                Iterable<JavaFileObject> snapshot = list(manager, blockedType);
                assertFalse(classNames(manager, snapshot).contains(blockedType.getName()));
                assertTrue(classNames(manager, list(manager, otherType)).contains(otherType.getName()));

                loader.resume.countDown();
                first.get(10, TimeUnit.SECONDS);

                assertTrue(classNames(manager, list(manager, blockedType)).contains(blockedType.getName()));
                assertTrue(classNames(manager, list(manager, otherType)).contains(otherType.getName()));
                assertFalse("An already returned listing must remain a snapshot",
                        classNames(manager, snapshot).contains(blockedType.getName()));
            } finally {
                loader.resume.countDown();
            }
        } finally {
            loader.resume.countDown();
            executor.shutdownNow();
            assertTrue("Registration workers did not terminate", executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static Iterable<JavaFileObject> list(MyJavaFileManager manager, Class<?> type) throws IOException {
        String name = type.getName();
        return manager.list(StandardLocation.CLASS_PATH, name.substring(0, name.lastIndexOf('.')),
                EnumSet.of(JavaFileObject.Kind.CLASS), false);
    }

    private static Set<String> classNames(MyJavaFileManager manager, Iterable<JavaFileObject> files) {
        Set<String> names = new HashSet<>();
        files.forEach(file -> names.add(manager.inferBinaryName(StandardLocation.CLASS_PATH, file)));
        return names;
    }

    private static final class BlockingLoader extends ClassLoader {
        private final CountDownLatch resolving = new CountDownLatch(1);
        private final CountDownLatch resume = new CountDownLatch(1);

        private BlockingLoader() {
            super(BlockingValue.class.getClassLoader());
        }

        private Class<?> defineValue() throws IOException {
            try (InputStream input = BlockingValue.class.getResourceAsStream("BlockingValue.class")) {
                assertNotNull(input);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) != -1)
                    output.write(buffer, 0, length);
                byte[] bytes = output.toByteArray();
                return defineClass(BlockingValue.class.getName(), bytes, 0, bytes.length);
            }
        }

        @Override
        public URL getResource(String name) {
            if (name.equals(BlockingValue.class.getName().replace('.', '/') + ".class")) {
                resolving.countDown();
                try {
                    assertTrue("Class-resource lookup was not resumed", resume.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            return super.getResource(name);
        }
    }
}
