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
public class ValuesJavaFileManager extends net.openhft.compiler.MyJavaFileManager {

    /*
     * Cache of classes required by generated code, keyed by package name.
     * Populated once in the static block below and shared across all
     * instances.
     */
    private static final Map<String, Set<JavaFileObject>> dependencyFileObjects = new HashMap<>();

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
    private final Map<String, Set<JavaFileObject>> fileObjects;

    /**
     * Creates a manager that serves the given {@code valueType} and all
     * dependencies from memory.
     */
    public ValuesJavaFileManager(Class<?> valueType, StandardJavaFileManager fileManager) {
        super(fileManager);
        // deep clone dependencyFileObjects
        fileObjects = new HashMap<>(dependencyFileObjects);
        fileObjects.replaceAll((p, objects) -> new HashSet<>(objects));
        // enrich with valueType's fileObjects
        addFileObjects(fileObjects, valueType);
    }

    /**
     * Adds the class so later compilations can reference it.
     */
    public void addClassToFileObjects(Class<?> c) {
        addFileObjects(fileObjects, c);
    }

    /**
     * Records the class and any interfaces it implements under their packages.
     */
    private static void addFileObjects(Map<String, Set<JavaFileObject>> fileObjects, Class<?> c) {
        fileObjects.compute(Jvm.getPackageName(c), (p, objects) -> {
            if (objects == null)
                objects = new HashSet<>();
            objects.add(classFileObject(c));
            return objects;
        });

        Type[] interfaces = c.getGenericInterfaces();
        for (Type superInterface : interfaces) {
            Class<?> rawInterface = ValueModel.rawInterface(superInterface);
            addFileObjects(fileObjects, rawInterface);
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
        Collection<JavaFileObject> packageFileObjects;
        if ((packageFileObjects = fileObjects.get(packageName)) != null) {
            packageFileObjects = new ArrayList<>(packageFileObjects);
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
