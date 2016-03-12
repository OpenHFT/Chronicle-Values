/*
 *      Copyright (C) 2015  higherfrequencytrading.com
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
import net.openhft.chronicle.core.ReferenceCounted;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@SuppressWarnings("RefusedBequest")
class MyJavaFileManager implements JavaFileManager {

    private static Stream<JavaFileObject> classFileObjects(Class<?> c) {
        List<JavaFileObject> fileObjects = new ArrayList<>();
        addFileObjects(fileObjects, c);
        return fileObjects.stream();
    }

    private static void addFileObjects(List<JavaFileObject> fileObjects, Class<?> c) {
        fileObjects.add(classFileObject(c));

        Type[] interfaces = c.getGenericInterfaces();
        for (Type superInterface : interfaces) {
            Class rawInterface = ValueModel.rawInterface(superInterface);
            if (rawInterface.getPackage().equals(c.getPackage()))
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

    private static Map.Entry<Package, List<JavaFileObject>> classesToFileObjects(
            List<Class<?>> classes) {
        List<JavaFileObject> fileObjects = classes.stream()
                .flatMap(MyJavaFileManager::classFileObjects)
                .distinct().collect(Collectors.toList());
        return new AbstractMap.SimpleEntry<>(classes.get(0).getPackage(), fileObjects);
    }

    private static final Map.Entry<Package, List<JavaFileObject>> valuesPackageFileObjects =
            classesToFileObjects(asList(Enums.class, CharSequences.class, ValueModel.class,
                    Values.class, FieldModel.class, ArrayFieldModel.class, Copyable.class));

    private static final Map.Entry<Package, List<JavaFileObject>> bytesPackageFileObjects =
            classesToFileObjects(asList(Bytes.class, BytesStore.class, BytesUtil.class,
                    Byteable.class, BytesMarshallable.class));

    private static final Map.Entry<Package, List<JavaFileObject>> corePackageFileObjects =
            classesToFileObjects(singletonList(ReferenceCounted.class));

    private static final Map.Entry<Package, List<JavaFileObject>> coreIoPackageFileObjects =
            classesToFileObjects(asList(Closeable.class, IORuntimeException.class));

    private static void addToPackages(
            Map<Package, List<JavaFileObject>> packages,
            Map.Entry<Package, List<JavaFileObject>> fileObjects) {
        packages.put(fileObjects.getKey(), fileObjects.getValue());
    }

    private final Map<Package, List<JavaFileObject>> packages = new HashMap<>();
    private final StandardJavaFileManager fileManager;
    private final Map<String, ByteArrayOutputStream> buffers = new LinkedHashMap<>();

    MyJavaFileManager(Class valueType, StandardJavaFileManager fileManager) {
        Map.Entry<Package, List<JavaFileObject>> valueTypePackageFileObjects =
                classesToFileObjects(singletonList(valueType));
        addToPackages(packages, coreIoPackageFileObjects);
        addToPackages(packages, corePackageFileObjects);
        addToPackages(packages, bytesPackageFileObjects);
        addToPackages(packages, valuesPackageFileObjects);
        addToPackages(packages, valueTypePackageFileObjects);
        this.fileManager = fileManager;
    }

    public ClassLoader getClassLoader(Location location) {
        return fileManager.getClassLoader(location);
    }

    public Iterable<JavaFileObject> list(
            Location location, String packageName, Set<Kind> kinds, boolean recurse)
            throws IOException {
        List<JavaFileObject> fileObjects = new ArrayList<>();
        for (Map.Entry<Package, List<JavaFileObject>> packageFileObjects : packages.entrySet()) {
            if (packageName.equals(packageFileObjects.getKey().getName())) {
                fileObjects.addAll(packageFileObjects.getValue());
                break;
            }
        }
        fileManager.list(location, packageName, kinds, recurse).forEach(fileObjects::add);
        return fileObjects;
    }

    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof SimpleURIClassObject) {
            return ((SimpleURIClassObject) file).c.getName();
        } else {
            return fileManager.inferBinaryName(location, file);
        }
    }

    public boolean isSameFile(FileObject a, FileObject b) {
        return fileManager.isSameFile(a, b);
    }

    public boolean handleOption(String current, Iterator<String> remaining) {
        return fileManager.handleOption(current, remaining);
    }

    public boolean hasLocation(Location location) {
        return fileManager.hasLocation(location);
    }

    public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind)
            throws IOException {
        if (location == StandardLocation.CLASS_OUTPUT && buffers.containsKey(className) &&
                kind == Kind.CLASS) {
            final byte[] bytes = buffers.get(className).toByteArray();
            return new SimpleJavaFileObject(URI.create(className), kind) {
                @NotNull
                public InputStream openInputStream() {
                    return new ByteArrayInputStream(bytes);
                }
            };
        }
        return fileManager.getJavaFileForInput(location, className, kind);
    }

    @NotNull
    public JavaFileObject getJavaFileForOutput(
            Location location, final String className, Kind kind, FileObject sibling)
            throws IOException {
        return new SimpleJavaFileObject(URI.create(className), kind) {
            @NotNull
            public OutputStream openOutputStream() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                buffers.put(className, baos);
                return baos;
            }
        };
    }

    public FileObject getFileForInput(
            Location location, String packageName, String relativeName) throws IOException {
        return fileManager.getFileForInput(location, packageName, relativeName);
    }

    public FileObject getFileForOutput(
            Location location, String packageName, String relativeName, FileObject sibling)
            throws IOException {
        return fileManager.getFileForOutput(location, packageName, relativeName, sibling);
    }

    public void flush() throws IOException {
        // Do nothing
    }

    public void close() throws IOException {
        fileManager.close();
    }

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
