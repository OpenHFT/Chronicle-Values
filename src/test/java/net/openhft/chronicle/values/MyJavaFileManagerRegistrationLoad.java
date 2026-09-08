/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import org.junit.rules.TemporaryFolder;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static net.openhft.chronicle.values.RegistrationTestSupport.*;
import static org.junit.Assert.*;

/**
 * Opt-in bounded registration/listing load, not a microbenchmark or a wall-clock CI gate.
 * Arguments: shared|isolated|cold, worker count (1-8), measured batches (1-100).
 * Run cold compilation in its own JVM: the runtime compiler retains generated sources.
 */
public final class MyJavaFileManagerRegistrationLoad {
    private MyJavaFileManagerRegistrationLoad() {
    }

    public static void main(String[] args) throws Exception {
        String scenario = args.length > 0 ? args[0] : "shared";
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        int batches = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        if (!(scenario.equals("shared") || scenario.equals("isolated") || scenario.equals("cold"))
                || threads < 1 || threads > 8 || batches < 1 || batches > 100)
            throw new IllegalArgumentException("Expected shared|isolated|cold, 1-8 workers, 1-100 batches");
        Map<String, String> sources = diamond(4);
        if (scenario.equals("isolated")) {
            sources.clear();
            for (int i = 0; i < 128; i++)
                addInterface(sources, PACKAGE + ".p" + (i % 8) + ".Isolated" + i,
                        "", "int getValue(); void setValue(int value);");
        }
        TemporaryFolder temporary = TemporaryFolder.builder().assureDeletion().build();
        temporary.create();
        try (Loader loader = compile(temporary.newFolder(), sources)) {
            if (scenario.equals("cold")) {
                Class<?> leaf = loader.type(PACKAGE + ".Left4");
                Object value = Values.newHeapInstance(leaf);
                leaf.getMethod("setValue", int.class).invoke(value, 42);
                assertEquals(42, leaf.getMethod("getValue").invoke(value));
                System.out.println("Cold Values heap compilation: diamond interface passed getter/setter round trip");
                return;
            }
            List<Class<?>> registrations = new ArrayList<>();
            for (int i = 0; i < 128; i++)
                registrations.add(loader.type(scenario.equals("shared") ? PACKAGE + ".Left4"
                        : PACKAGE + ".p" + (i % 8) + ".Isolated" + i));
            Set<Class<?>> expected = new HashSet<>();
            Set<String> packages = new TreeSet<>();
            for (Class<?> type : registrations)
                expected.addAll(closure(type));
            for (Class<?> type : expected)
                packages.add(type.getName().substring(0, type.getName().lastIndexOf('.')));
            System.out.println("JVM: " + System.getProperty("java.runtime.version"));
            System.out.println("Registry: " + MyJavaFileManager.class.getProtectionDomain().getCodeSource().getLocation());
            System.out.println("scenario,workers,batch,registrations,batch_ms,p95_registration_us,resource_lookups,retained_objects,distinct_classes");
            for (int batch = -3; batch < batches; batch++) {
                loader.lookups.values().forEach(count -> count.set(0));
                try (StandardJavaFileManager standard = emptyDelegate();
                     Workers pool = new Workers(threads)) {
                    MyJavaFileManager manager = new MyJavaFileManager(Values.class, standard);
                    CountDownLatch ready = new CountDownLatch(threads);
                    CountDownLatch start = new CountDownLatch(1);
                    List<Future<List<Long>>> futures = new ArrayList<>();
                    for (int worker = 0; worker < threads; worker++) {
                        int offset = worker;
                        futures.add(pool.executor.submit(() -> {
                            List<Long> latencies = new ArrayList<>();
                            ready.countDown();
                            assertTrue(start.await(10, TimeUnit.SECONDS));
                            for (int i = offset; i < registrations.size(); i += threads) {
                                long started = System.nanoTime();
                                manager.addClassToFileObjects(registrations.get(i));
                                latencies.add(System.nanoTime() - started);
                            }
                            return latencies;
                        }));
                    }
                    assertTrue(ready.await(10, TimeUnit.SECONDS));
                    long started = System.nanoTime();
                    start.countDown();
                    List<Iterable<JavaFileObject>> snapshots = new ArrayList<>();
                    List<List<Class<?>>> snapshotClasses = new ArrayList<>();
                    for (int i = 0; i < 16; i++) {
                        Iterable<JavaFileObject> snapshot = list(manager, packages.iterator().next());
                        snapshots.add(snapshot);
                        snapshotClasses.add(classes(snapshot));
                    }
                    List<Long> latencies = new ArrayList<>();
                    for (Future<List<Long>> future : futures)
                        latencies.addAll(future.get(30, TimeUnit.SECONDS));
                    List<Class<?>> actual = new ArrayList<>();
                    for (String packageName : packages)
                        actual.addAll(classes(list(manager, packageName)));
                    long elapsed = System.nanoTime() - started;
                    assertEquals(expected, new HashSet<>(actual));
                    for (int i = 0; i < snapshots.size(); i++)
                        assertEquals(snapshotClasses.get(i), classes(snapshots.get(i)));
                    // Count duplicates rather than rejecting them, so the same harness can measure the baseline.
                    int lookups = loader.lookups.values().stream().mapToInt(count -> count.get()).sum();
                    Collections.sort(latencies);
                    if (batch >= 0)
                        System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%.3f,%.3f,%d,%d,%d%n",
                                scenario, threads, batch, registrations.size(), elapsed / 1e6,
                                latencies.get((latencies.size() * 95 - 1) / 100) / 1e3,
                                lookups, actual.size(), expected.size());
                }
            }
        } finally {
            temporary.delete();
        }
    }
}
