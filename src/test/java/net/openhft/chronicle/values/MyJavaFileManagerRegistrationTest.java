/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static net.openhft.chronicle.values.RegistrationTestSupport.*;
import static org.junit.Assert.*;

public class MyJavaFileManagerRegistrationTest {
    @Rule
    public final TemporaryFolder temporary = TemporaryFolder.builder().assureDeletion().build();

    @Test
    public void resolvesAndRetainsEachInheritedClassOnlyOnce() throws Exception {
        try (Loader loader = compile(temporary.newFolder(), diamond(4));
             StandardJavaFileManager standard = emptyDelegate()) {
            MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
            Class<?> leaf = loader.type(PACKAGE + ".Left4");
            Set<Class<?>> expected = closure(leaf);
            for (int i = 0; i < 10; i++)
                manager.addClassToFileObjects(leaf);
            assertRegistry(manager, expected);
            for (Class<?> type : expected)
                assertEquals(type.getName(), 1, loader.lookups(type));
        }
    }

    @Test
    public void traversesCachedDiamondsInBoundedWork() throws Exception {
        try (Loader loader = compile(temporary.newFolder(), diamond(8));
             StandardJavaFileManager standard = emptyDelegate()) {
            Class<?> leaf = loader.type(PACKAGE + ".Left8");
            MyJavaFileManager manager = new MyJavaFileManager(leaf, standard);
            // Count real cache operations without adding a production instrumentation hook.
            Field field = MyJavaFileManager.class.getDeclaredField("fileObjects");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> original = (Map<String, Object>) field.get(manager);
            CountingMap counted = new CountingMap(original);
            Method register = MyJavaFileManager.class.getDeclaredMethod("addFileObjects", Map.class, Class.class);
            register.setAccessible(true);
            register.invoke(null, counted, leaf);
            assertTrue("Cache operations follow inheritance paths: " + counted.operations,
                    counted.operations.get() <= 2 * closure(leaf).size());
        }
    }

    @Test
    public void preservesClassLoaderIdentity() throws Exception {
        Map<String, String> sources = diamond(0);
        try (Loader first = compile(temporary.newFolder(), sources);
             Loader second = compile(temporary.newFolder(), sources);
             StandardJavaFileManager standard = emptyDelegate()) {
            MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
            Class<?> firstRoot = first.type(PACKAGE + ".Root");
            Class<?> secondRoot = second.type(PACKAGE + ".Root");
            assertNotSame(firstRoot, secondRoot);
            manager.addClassToFileObjects(firstRoot);
            manager.addClassToFileObjects(secondRoot);
            assertRegistry(manager, new HashSet<>(Arrays.asList(firstRoot, secondRoot)));
        }
    }

    @Test
    public void keepsPreloadedPackagesIndependentBetweenManagers() throws Exception {
        String markerName = "net.openhft.chronicle.values.RegistrationMarker";
        Map<String, String> sources = new LinkedHashMap<>();
        addInterface(sources, markerName, "", "");
        try (Loader loader = compile(temporary.newFolder(), sources);
             StandardJavaFileManager firstDelegate = emptyDelegate();
             StandardJavaFileManager secondDelegate = emptyDelegate();
             StandardJavaFileManager thirdDelegate = emptyDelegate()) {
            MyJavaFileManager first = new MyJavaFileManager(Values.class, firstDelegate);
            MyJavaFileManager second = new MyJavaFileManager(Values.class, secondDelegate);
            Class<?> marker = loader.type(markerName);
            first.addClassToFileObjects(marker);
            MyJavaFileManager third = new MyJavaFileManager(Values.class, thirdDelegate);
            for (MyJavaFileManager manager : Arrays.asList(first, second, third)) {
                List<Class<?>> registered = classes(list(manager, "net.openhft.chronicle.values"));
                assertTrue(registered.contains(Values.class));
                assertEquals(manager == first, registered.contains(marker));
            }
        }
    }

    @Test
    public void retriesParentsAfterAnEarlierResourceFailure() throws Exception {
        try (Loader loader = compile(temporary.newFolder(), diamond(2));
             StandardJavaFileManager standard = emptyDelegate()) {
            MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
            Class<?> leaf = loader.type(PACKAGE + ".Left2");
            loader.failNext.set(Loader.resourceName(PACKAGE + ".Root"));
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> manager.addClassToFileObjects(leaf));
            assertTrue(failure.getMessage().startsWith("Injected resource failure:"));
            manager.addClassToFileObjects(leaf);
            assertRegistry(manager, closure(leaf));
            assertEquals("Already resolved child", 1, loader.lookups(leaf));
        }
    }

    @Test
    public void completesParentsWhileAnotherRegistrationIsBlocked() throws Exception {
        try (Loader loader = compile(temporary.newFolder(), diamond(2));
             StandardJavaFileManager standard = emptyDelegate();
             Workers workers = new Workers(2)) {
            ExecutorService executor = workers.executor;
            MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
            Class<?> leaf = loader.type(PACKAGE + ".Left2");
            loader.blockNext.set(Loader.resourceName(PACKAGE + ".Root"));
            Future<?> first = executor.submit(() -> manager.addClassToFileObjects(leaf));
            try {
                assertTrue(loader.blocked.await(10, TimeUnit.SECONDS));
                Future<?> second = executor.submit(() -> manager.addClassToFileObjects(leaf));
                second.get(10, TimeUnit.SECONDS);
                assertRegistry(manager, closure(leaf));
            } finally {
                loader.resume.countDown();
                first.get(10, TimeUnit.SECONDS);
            }
            assertRegistry(manager, closure(leaf));
        }
    }

    @Test
    public void retainsUniqueRegistrationsUnderContention() throws Exception {
        try (Loader loader = compile(temporary.newFolder(), generated(180L, 24, 4));
             StandardJavaFileManager standard = emptyDelegate();
             Workers pool = new Workers(4)) {
            ExecutorService executor = pool.executor;
            MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
            Class<?> root = loader.type(PACKAGE + ".Root");
            manager.addClassToFileObjects(root);
            Iterable<JavaFileObject> before = list(manager, PACKAGE);
            List<Class<?>> beforeClasses = classes(before);
            List<Class<?>> types = new ArrayList<>();
            Set<Class<?>> expected = new HashSet<>();
            for (int i = 0; i < 24; i++) {
                Class<?> type = loader.type(PACKAGE + ".p" + (i % 4) + ".Node" + i);
                types.add(type);
                expected.addAll(closure(type));
            }
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> workers = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                List<Class<?>> order = new ArrayList<>(types);
                Collections.shuffle(order, new Random(worker));
                workers.add(executor.submit(() -> {
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    for (int round = 0; round < 10; round++)
                        order.forEach(manager::addClassToFileObjects);
                    return null;
                }));
            }
            start.countDown();
            for (int i = 0; i < 32; i++) {
                List<Class<?>> during = classes(list(manager, PACKAGE));
                assertTrue(expected.containsAll(during));
                assertEquals(during.size(), new HashSet<>(during).size());
            }
            for (Future<?> worker : workers)
                worker.get(10, TimeUnit.SECONDS);
            assertRegistry(manager, expected);
            assertEquals("Returned snapshot changed", beforeClasses, classes(before));
        }
    }

    @Test
    public void retainsConcurrentAdditionsToPreloadedPackages() throws Exception {
        Map<String, String> sources = new LinkedHashMap<>();
        for (int i = 0; i < 96; i++)
            addInterface(sources, "net.openhft.chronicle.values.RegistrationMarker" + i, "", "");
        assertConcurrentAdditions(sources);
    }

    @Test
    public void retainsConcurrentAdditionsToNewPackages() throws Exception {
        Map<String, String> sources = new LinkedHashMap<>();
        for (int i = 0; i < 96; i++)
            addInterface(sources, PACKAGE + ".RegistrationMarker" + i, "", "");
        assertConcurrentAdditions(sources);
    }

    @Test
    public void retainsConcurrentAdditionsToCollidingPackages() throws Exception {
        Map<String, String> sources = new LinkedHashMap<>();
        for (int i = 0; i < 32; i++) {
            StringBuilder packageName = new StringBuilder(PACKAGE).append('.');
            // Aa and BB have the same String hash, forcing distinct packages into one cache bin.
            for (int bit = 0; bit < 5; bit++)
                packageName.append((i & (1 << bit)) == 0 ? "Aa" : "BB");
            addInterface(sources, packageName + ".Marker", "", "");
        }
        assertConcurrentAdditions(sources);
    }

    private void assertConcurrentAdditions(Map<String, String> sources) throws Exception {
        try (Loader loader = compile(temporary.newFolder(), sources);
             Workers pool = new Workers(8)) {
            List<Class<?>> types = new ArrayList<>();
            for (String name : sources.keySet())
                types.add(loader.type(name));
            loader.resourceBarrier = new CyclicBarrier(8);
            int rounds = Integer.getInteger("values.registration.contentionRounds", 8);
            assertTrue("Bound the contention campaign", rounds > 0 && rounds <= 256);
            for (int round = 0; round < rounds; round++) {
                try (StandardJavaFileManager standard = emptyDelegate()) {
                    MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
                    Set<Class<?>> expected = new HashSet<>(types);
                    Set<String> packages = new HashSet<>();
                    for (Class<?> type : types)
                        packages.add(type.getName().substring(0, type.getName().lastIndexOf('.')));
                    for (String packageName : packages)
                        expected.addAll(classes(list(manager, packageName)));
                    CountDownLatch ready = new CountDownLatch(8);
                    CountDownLatch start = new CountDownLatch(1);
                    List<Future<?>> futures = new ArrayList<>();
                    try {
                        for (int worker = 0; worker < 8; worker++) {
                            int offset = worker;
                            futures.add(pool.executor.submit(() -> {
                                ready.countDown();
                                assertTrue(start.await(10, TimeUnit.SECONDS));
                                // Each wave resolves distinct classes before contending on the cache.
                                for (int i = offset; i < types.size(); i += 8)
                                    manager.addClassToFileObjects(types.get(i));
                                return null;
                            }));
                        }
                        assertTrue(ready.await(10, TimeUnit.SECONDS));
                        start.countDown();
                        for (Future<?> future : futures)
                            future.get(10, TimeUnit.SECONDS);
                        assertRegistry(manager, expected);
                    } finally {
                        start.countDown();
                        for (Future<?> future : futures)
                            future.cancel(true);
                    }
                }
            }
        }
    }

    @Test
    public void seededRegistrationSequencesMatchTheirInterfaceClosure() throws Exception {
        long[] regressions = {0L, 180L, 842L, 0x5eedL};
        Random campaign = new Random(180L);
        int cases = Integer.getInteger("values.registration.fuzzCases", regressions.length);
        assertTrue("Bound the generated campaign", cases > 0 && cases <= 1024);
        for (int run = 0; run < cases; run++) {
            long seed = run < regressions.length ? regressions[run] : campaign.nextLong();
            Map<String, String> sources = generated(seed, 24, 4);
            try (Loader loader = compile(temporary.newFolder(), sources);
                 StandardJavaFileManager standard = emptyDelegate()) {
                MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
                Set<Class<?>> expected = new HashSet<>();
                List<Integer> trace = new ArrayList<>();
                Random random = new Random(seed);
                for (int step = 0; step < 64; step++) {
                    int index = random.nextInt(24);
                    trace.add(index);
                    Class<?> type = loader.type(PACKAGE + ".p" + (index % 4) + ".Node" + index);
                    String packageName = type.getName().substring(0, type.getName().lastIndexOf('.'));
                    Iterable<JavaFileObject> snapshot = list(manager, packageName);
                    List<Class<?>> snapshotClasses = classes(snapshot);
                    manager.addClassToFileObjects(type);
                    expected.addAll(closure(type));
                    try {
                        assertRegistry(manager, expected);
                        assertEquals(snapshotClasses, classes(snapshot));
                        for (Class<?> registered : expected)
                            assertEquals(registered.getName(), 1, loader.lookups(registered));
                    } catch (AssertionError failure) {
                        throw new AssertionError("Seed " + seed + ", registration trace " + trace
                                + ", sources " + sources, failure);
                    }
                }
            }
        }
    }

    private static final class CountingMap extends ConcurrentHashMap<String, Object> {
        private static final long serialVersionUID = 1L;
        private final AtomicInteger operations = new AtomicInteger();

        CountingMap(Map<String, Object> source) {
            super(source);
        }

        @Override
        public Object get(Object key) {
            operations.incrementAndGet();
            return super.get(key);
        }

        @Override
        public Object computeIfAbsent(String key, Function<? super String, ?> function) {
            operations.incrementAndGet();
            return super.computeIfAbsent(key, function);
        }
    }
}
