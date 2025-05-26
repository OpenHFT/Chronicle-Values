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

public class MyJavaFileManager extends net.openhft.compiler.MyJavaFileManager {

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
                IORuntimeException.class, InvalidMarshallableException.class,
                ClosedIllegalStateException.class, ThreadingIllegalStateException.class

        ).forEach(c -> addFileObjects(dependencyFileObjects, c));
    }

    private final Map<String, Set<JavaFileObject>> fileObjects;

    public MyJavaFileManager(Class<?> valueType, StandardJavaFileManager fileManager) {
        super(fileManager);
        // deep clone dependencyFileObjects
        fileObjects = new HashMap<>(dependencyFileObjects);
        fileObjects.replaceAll((p, objects) -> new HashSet<>(objects));
        // enrich with valueType's fileObjects
        addFileObjects(fileObjects, valueType);
    }

    public void addClassToFileObjects(Class<?> c) {
        addFileObjects(fileObjects, c);
    }

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
            throw new RuntimeException(e);
        }
    }

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

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof SimpleURIClassObject) {
            return ((SimpleURIClassObject) file).c.getName();
        }
        return super.inferBinaryName(location, file);
    }

}
