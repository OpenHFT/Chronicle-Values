/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

final class RegistrationTestSupport {
    static final String PACKAGE = "net.openhft.chronicle.values.filemanager.generated";

    private RegistrationTestSupport() {
    }

    static Map<String, String> diamond(int depth) {
        Map<String, String> sources = new LinkedHashMap<>();
        addInterface(sources, PACKAGE + ".Root", "", "int getValue(); void setValue(int value);");
        for (int i = 0; i <= depth; i++) {
            String parents = i == 0 ? "Root" : "Left" + (i - 1) + ", Right" + (i - 1);
            addInterface(sources, PACKAGE + ".Left" + i, parents, "");
            addInterface(sources, PACKAGE + ".Right" + i, parents, "");
        }
        return sources;
    }

    static Map<String, String> generated(long seed, int size, int packages) {
        Random random = new Random(seed);
        Map<String, String> sources = new LinkedHashMap<>();
        List<String> parents = new ArrayList<>();
        parents.add(PACKAGE + ".Root");
        addInterface(sources, parents.get(0), "", "int getValue(); void setValue(int value);");
        addInterface(sources, PACKAGE + ".Generic<T>", "", "");
        for (int i = 0; i < size; i++) {
            Set<String> selected = new LinkedHashSet<>();
            selected.add(parents.get(random.nextInt(parents.size())));
            selected.add(parents.get(random.nextInt(parents.size())));
            if (i % 3 == 0)
                selected.add(PACKAGE + ".Generic<String>");
            String name = PACKAGE + ".p" + (i % packages) + ".Node" + i;
            addInterface(sources, name, String.join(", ", selected), "");
            parents.add(name);
        }
        return sources;
    }

    static void addInterface(Map<String, String> sources, String name, String parents, String body) {
        int dot = name.lastIndexOf('.');
        String source = "package " + name.substring(0, dot) + "; public interface "
                + name.substring(dot + 1) + (parents.isEmpty() ? "" : " extends " + parents)
                + " { " + body + " }";
        sources.put(name.replace("<T>", ""), source);
    }

    static Loader compile(File directory, Map<String, String> sources) throws IOException {
        List<JavaFileObject> units = new ArrayList<>();
        sources.forEach((name, source) -> units.add(new SimpleJavaFileObject(
                URI.create("string:///" + name.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        }));
        try (StandardJavaFileManager standard = emptyDelegate()) {
            standard.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(directory));
            assertTrue("Unable to compile generated fixture: " + sources,
                    ToolProvider.getSystemJavaCompiler().getTask(null, standard, null,
                            Arrays.asList("-proc:none", "-source", "8", "-target", "8", "-Xlint:-options"),
                            null, units).call());
        }
        return new Loader(directory, sources.keySet());
    }

    static StandardJavaFileManager emptyDelegate() throws IOException {
        StandardJavaFileManager standard = ToolProvider.getSystemJavaCompiler()
                .getStandardFileManager(null, null, null);
        standard.setLocation(StandardLocation.CLASS_PATH, Collections.emptyList());
        return standard;
    }

    static Set<Class<?>> closure(Class<?> type) {
        Set<Class<?>> result = new HashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        pending.add(type);
        while (!pending.isEmpty()) {
            Class<?> next = pending.removeFirst();
            if (result.add(next))
                Collections.addAll(pending, next.getInterfaces());
        }
        return result;
    }

    static Iterable<JavaFileObject> list(MyJavaFileManager manager, String packageName) throws IOException {
        return manager.list(StandardLocation.CLASS_PATH, packageName,
                EnumSet.of(JavaFileObject.Kind.CLASS), false);
    }

    static List<Class<?>> classes(Iterable<JavaFileObject> files) {
        List<Class<?>> classes = new ArrayList<>();
        for (JavaFileObject file : files)
            classes.add(((SimpleURIClassObject) file).c);
        return classes;
    }

    static void assertRegistry(MyJavaFileManager manager, Set<Class<?>> expected) throws IOException {
        Set<String> packages = new HashSet<>();
        for (Class<?> type : expected)
            packages.add(type.getName().substring(0, type.getName().lastIndexOf('.')));
        List<Class<?>> actual = new ArrayList<>();
        for (String packageName : packages)
            actual.addAll(classes(list(manager, packageName)));
        assertEquals("Registered class identities", expected, new HashSet<>(actual));
        assertEquals("Redundant class-file objects", expected.size(), actual.size());
    }

    static final class Loader extends URLClassLoader {
        final Map<String, AtomicInteger> lookups = new ConcurrentHashMap<>();
        final AtomicReference<String> failNext = new AtomicReference<>();
        final AtomicReference<String> blockNext = new AtomicReference<>();
        final CountDownLatch blocked = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        volatile CyclicBarrier resourceBarrier;

        Loader(File directory, Collection<String> names) throws IOException {
            super(new URL[]{directory.toURI().toURL()}, RegistrationTestSupport.class.getClassLoader());
            for (String name : names)
                lookups.put(resourceName(name), new AtomicInteger());
        }

        Class<?> type(String name) throws ClassNotFoundException {
            return Class.forName(name, false, this);
        }

        int lookups(Class<?> type) {
            return lookups.get(resourceName(type.getName())).get();
        }

        @Override
        public URL getResource(String name) {
            AtomicInteger count = lookups.get(name);
            if (count != null)
                count.incrementAndGet();
            String failure = failNext.get();
            if (name.equals(failure) && failNext.compareAndSet(failure, null))
                throw new IllegalStateException("Injected resource failure: " + name);
            String blocking = blockNext.get();
            if (name.equals(blocking) && blockNext.compareAndSet(blocking, null)) {
                blocked.countDown();
                try {
                    assertTrue("Resource lookup was not resumed", resume.await(10, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            URL resource = super.getResource(name);
            CyclicBarrier barrier = resourceBarrier;
            if (barrier != null && count != null) {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
            return resource;
        }

        static String resourceName(String name) {
            return name.replace('.', '/') + ".class";
        }
    }

    static final class Workers implements AutoCloseable {
        final ExecutorService executor;

        Workers(int threads) {
            executor = Executors.newFixedThreadPool(threads);
        }

        @Override
        public void close() {
            executor.shutdownNow();
            try {
                assertTrue("Registration workers did not terminate", executor.awaitTermination(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }
}
