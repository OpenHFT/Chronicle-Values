/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@SuppressWarnings("RefusedBequest")
class MyJavaFileManager implements JavaFileManager {

    private static final Map<String, Set<JavaFileObject>> dependencyFileObjects = new HashMap<>();

    static {
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

                // Core exception
                IORuntimeException.class

        ).forEach(c -> addFileObjects(dependencyFileObjects, c));
    }

    private final Map<String, Set<JavaFileObject>> fileObjects;
    private final StandardJavaFileManager fileManager;
    private final Map<String, ByteArrayOutputStream> buffers = new LinkedHashMap<>();

    MyJavaFileManager(Class valueType, StandardJavaFileManager fileManager) {
        // deep clone dependencyFileObjects
        fileObjects = new HashMap<>(dependencyFileObjects);
        fileObjects.replaceAll((p, objects) -> new HashSet<>(objects));
        // enrich with valueType's fileObjects
        addFileObjects(fileObjects, valueType);
        this.fileManager = fileManager;
    }

    private static void addFileObjects(Map<String, Set<JavaFileObject>> fileObjects, Class<?> c) {
        fileObjects.compute(c.getPackage().getName(), (p, objects) -> {
            if (objects == null)
                objects = new HashSet<>();
            objects.add(classFileObject(c));
            return objects;
        });

        Type[] interfaces = c.getGenericInterfaces();
        for (Type superInterface : interfaces) {
            Class rawInterface = ValueModel.rawInterface(superInterface);
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
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return fileManager.getClassLoader(location);
    }

    @Override
    public Iterable<JavaFileObject> list(
            Location location, String packageName, Set<Kind> kinds, boolean recurse)
            throws IOException {
        Iterable<JavaFileObject> delegateFileObjects =
                fileManager.list(location, packageName, kinds, recurse);
        Collection<JavaFileObject> packageFileObjects;
        if ((packageFileObjects = fileObjects.get(packageName)) != null) {
            packageFileObjects = new ArrayList<>(packageFileObjects);
            delegateFileObjects.forEach(packageFileObjects::add);
            return packageFileObjects;
        } else {
            return delegateFileObjects;
        }
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof SimpleURIClassObject) {
            return ((SimpleURIClassObject) file).c.getName();
        } else {
            return fileManager.inferBinaryName(location, file);
        }
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        return fileManager.isSameFile(a, b);
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return fileManager.handleOption(current, remaining);
    }

    @Override
    public boolean hasLocation(Location location) {
        return fileManager.hasLocation(location);
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind)
            throws IOException {
        if (location == StandardLocation.CLASS_OUTPUT && buffers.containsKey(className) &&
                kind == Kind.CLASS) {
            final byte[] bytes = buffers.get(className).toByteArray();
            return new SimpleJavaFileObject(URI.create(className), kind) {
                @Override
                @NotNull
                public InputStream openInputStream() {
                    return new ByteArrayInputStream(bytes);
                }
            };
        }
        return fileManager.getJavaFileForInput(location, className, kind);
    }

    @Override
    @NotNull
    public JavaFileObject getJavaFileForOutput(
            Location location, final String className, Kind kind, FileObject sibling)
            throws IOException {
        return new SimpleJavaFileObject(URI.create(className), kind) {
            @Override
            @NotNull
            public OutputStream openOutputStream() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                buffers.put(className, baos);
                return baos;
            }
        };
    }

    @Override
    public FileObject getFileForInput(
            Location location, String packageName, String relativeName) throws IOException {
        return fileManager.getFileForInput(location, packageName, relativeName);
    }

    @Override
    public FileObject getFileForOutput(
            Location location, String packageName, String relativeName, FileObject sibling)
            throws IOException {
        return fileManager.getFileForOutput(location, packageName, relativeName, sibling);
    }

    @Override
    public void flush() throws IOException {
        // Do nothing
    }

    @Override
    public void close() throws IOException {
        fileManager.close();
    }

    @Override
    public int isSupportedOption(String option) {
        return fileManager.isSupportedOption(option);
    }

    @NotNull
    public Map<String, byte[]> getAllBuffers() {
        Map<String, byte[]> ret = new LinkedHashMap<>(buffers.size() * 2);
        for (Map.Entry<String, ByteArrayOutputStream> entry : buffers.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().toByteArray());
        }
        return ret;
    }

    /*
     * Java 9+
     */

    public Location getLocationForModule(Location location, String moduleName) {
        try {
            Method getLocationForModule = Jvm.getMethod(JavaFileManager.class, "getLocationForModule", Location.class, String.class);
            return (Location) getLocationForModule.invoke(fileManager, location, moduleName);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    public Location getLocationForModule(Location location, JavaFileObject fo) {
        try {
            Method getLocationForModule = Jvm.getMethod(JavaFileManager.class, "getLocationForModule", Location.class, JavaFileObject.class);
            return (Location) getLocationForModule.invoke(fileManager, location, fo);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) {
        try {
            Method getServiceLoader = Jvm.getMethod(JavaFileManager.class, "getServiceLoader", Location.class, Class.class);
            return (ServiceLoader<S>) getServiceLoader.invoke(fileManager, location, service);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    public String inferModuleName(Location location) {
        try {
            Method inferModuleName = Jvm.getMethod(JavaFileManager.class, "inferModuleName", Location.class);
            return (String) inferModuleName.invoke(fileManager, location);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    public Iterable<Set<Location>> listLocationsForModules(Location location) {
        try {
            Method listLocationsForModules = Jvm.getMethod(JavaFileManager.class, "listLocationsForModules", Location.class);
            return (Iterable<Set<Location>>) listLocationsForModules.invoke(fileManager, location);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    public boolean contains(Location location, FileObject fo) {
        try {
            Method contains = Jvm.getMethod(JavaFileManager.class, "contains", Location.class, JavaFileObject.class);
            return (Boolean) contains.invoke(fileManager, location, fo);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }
}
