/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Supplies in-memory {@link JavaFileObject} instances to the compiler.
 *
 * <p>The manager collects class files for the Values API and the target
 * interface so the {@code JavaCompiler} resolves them without touching
 * disk.  Dependencies are cached in a static map,
 * {@code dependencyFileObjects}, which is filled once with Chronicle
 * classes required by generated code.  Each manager instance clones that
 * map into its own {@code fileObjects} cache and augments it with the
 * classes for the specific value type being compiled.  This approach
 * avoids repeated lookups and keeps compilation entirely in memory.
 */
public class MyJavaFileManager extends net.openhft.compiler.MyJavaFileManager {

    /*
     * Cache of classes required by generated code, keyed by package name.
     * Populated once in the static block below and shared across all
     * instances.
     */
    private static final Map<String, Map<Class<?>, JavaFileObject>> dependencyFileObjects = new HashMap<>();

    /*
     * Preloads {@code dependencyFileObjects} with Chronicle classes used by
     * generated code.
     */
    static {
        // Chronicle classes commonly referenced by generated implementations
        Arrays.asList(
                // Values classes and interfaces
                Enums.class, CharSequences.class, ValueModel.class,
                Values.class, FieldModel.class, ArrayFieldModel.class, Copyable.class,

                // Values annotations
                Align.class, Array.class, Group.class, MaxUtf8Length.class,
                net.openhft.chronicle.values.NotNull.class, Range.class,

                // Bytes classes and interfaces
                Bytes.class, BytesStore.class, BytesUtil.class,
                Byteable.class, BytesMarshallable.class,

                // Core exceptions
                IORuntimeException.class, InvalidMarshallableException.class,
                ClosedIllegalStateException.class, ThreadingIllegalStateException.class

        ).forEach(c -> addFileObjects(dependencyFileObjects, c));
    }

    /**
     * Per-instance cache initially containing a deep copy of
     * {@code dependencyFileObjects}.  Additional classes for the target
     * interface are recorded here so they are visible to the compiler.
     */
    private final Map<String, Map<Class<?>, JavaFileObject>> fileObjects;

    /**
     * Creates a manager that serves the given {@code valueType} and all
     * dependencies from memory.
     */
    public MyJavaFileManager(Class<?> valueType, StandardJavaFileManager fileManager) {
        super(fileManager);
        //! Registrations can overlap on a shared manager, so both map levels must be concurrent.
        //! Copy each package map so registering a class cannot mutate another manager's preloaded registry.
        fileObjects = new ConcurrentHashMap<>(dependencyFileObjects);
        fileObjects.replaceAll((p, objects) -> new ConcurrentHashMap<>(objects));
        // enrich with valueType's fileObjects
        addFileObjects(fileObjects, valueType);
    }

    /**
     * Adds the class and its inherited interfaces, retaining one object per class identity.
     * Concurrent calls may resolve the same resource before either publishes it.
     */
    public void addClassToFileObjects(Class<?> c) {
        addFileObjects(fileObjects, c);
    }

    /**
     * Records the class and any interfaces it implements under their packages.
     */
    private static void addFileObjects(Map<String, Map<Class<?>, JavaFileObject>> fileObjects, Class<?> c) {
        addFileObjects(fileObjects, c, new HashSet<>());
    }

    private static void addFileObjects(Map<String, Map<Class<?>, JavaFileObject>> fileObjects,
                                       Class<?> c, Set<Class<?>> visited) {
        //! A diamond reaches shared ancestors through several paths; tracking a visit per call bounds
        //! traversal by distinct classes and inheritance edges rather than the number of paths.
        if (!visited.add(c))
            return;

        //! Fresh wrappers have identity equality, so a wrapper set retains duplicates of the same class.
        //! Class keys deduplicate registrations without merging equal binary names from different defining loaders.
        String packageName = Jvm.getPackageName(c);
        Map<Class<?>, JavaFileObject> packageObjects = fileObjects.get(packageName);
        if (packageObjects == null || !packageObjects.containsKey(c)) {
            //! Class-resource lookup invokes class-loader code and may block or re-enter registration.
            //! Keep it outside map callbacks so resource resolution does not hold the cache's remapping locks.
            //! Concurrent cold lookups may overlap; putIfAbsent retains just one wrapper for this class.
            JavaFileObject fileObject = classFileObject(c);
            fileObjects.computeIfAbsent(packageName, p -> new ConcurrentHashMap<>())
                    .putIfAbsent(c, fileObject);
        }

        //! A cached child can belong to an incomplete or failed registration; it is not a completion marker.
        //! Traverse its parents even on a cache hit so this call can complete the inherited dependency closure.
        Type[] interfaces = c.getGenericInterfaces();
        for (Type superInterface : interfaces) {
            Class<?> rawInterface = ValueModel.rawInterface(superInterface);
            addFileObjects(fileObjects, rawInterface, visited);
        }
    }

    private static JavaFileObject classFileObject(Class<?> c) {
        try {
            String className = c.getName();
            int lastDotIndex = className.lastIndexOf('.');
            URI uri = c.getResource(className.substring(lastDotIndex + 1) + ".class").toURI();
            return new SimpleURIClassObject(uri, c);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to resolve class URI for " + c.getName(), e);
        }
    }

    /**
     * Returns the union of delegate and in-memory objects for the package.
     */
    @Override
    public Iterable<JavaFileObject> list(
            Location location, String packageName, Set<Kind> kinds, boolean recurse)
            throws IOException {
        Iterable<JavaFileObject> delegateFileObjects =
                super.list(location, packageName, kinds, recurse);
        Map<Class<?>, JavaFileObject> packageObjects = fileObjects.get(packageName);
        if (packageObjects != null) {
            //! Copy registry entries so later registrations cannot change an already returned listing.
            //! Iteration is weakly consistent; this does not make multi-class registration atomic.
            Collection<JavaFileObject> packageFileObjects = new ArrayList<>(packageObjects.values());
            delegateFileObjects.forEach(packageFileObjects::add);
            return packageFileObjects;
        } else {
            return delegateFileObjects;
        }
    }

    /**
     * Derives the binary name for a {@link SimpleURIClassObject}.
     */
    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof SimpleURIClassObject) {
            return ((SimpleURIClassObject) file).c.getName();
        }
        return super.inferBinaryName(location, file);
    }

}
