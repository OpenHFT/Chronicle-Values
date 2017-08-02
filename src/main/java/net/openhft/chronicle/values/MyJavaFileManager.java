/*
 *      Copyright (C) 2015, 2016  higherfrequencytrading.com
 *      Copyright (C) 2016 Roman Leventov
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@SuppressWarnings("RefusedBequest")
class MyJavaFileManager implements JavaFileManager {

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
}
