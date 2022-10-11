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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.CompilerUtils;
import net.openhft.chronicle.core.util.WeakIdentityHashMap;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("StaticNonFinalField")
class CachedCompiler {
    public static final CachedCompiler CACHED_COMPILER = new CachedCompiler();

    private static final Map<ClassLoader, Map<String, Class>> loadedClassesMap =
            new WeakIdentityHashMap<>();

    private static final List<String> java8Options =
            Arrays.asList("-XDenableSunApiLintControl", "-Xlint:-sunapi");
    private static final List<String> java9PlusOptions = null;

    private static JavaCompiler compiler;
    private static StandardJavaFileManager standardJavaFileManager;
    private final Map<String, JavaFileObject> javaFileObjects = new HashMap<>();
    private boolean errors;

    static {
        reset();
    }

    private static void reset() {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            try {
                Class<?> javacTool = Class.forName("com.sun.tools.javac.api.JavacTool");
                Method create = javacTool.getMethod("create");
                compiler = (JavaCompiler) create.invoke(null);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        standardJavaFileManager = compiler.getStandardFileManager(null, null, null);
    }

    @NotNull
    Map<String, byte[]> compileFromJava(
            Class valueType, @NotNull String className, @NotNull String javaCode) {
        Iterable<? extends JavaFileObject> compilationUnits;
        javaFileObjects.put(className, new JavaSourceFromString(className, javaCode));
        compilationUnits = javaFileObjects.values();

        MyJavaFileManager fileManager =
                new MyJavaFileManager(valueType, standardJavaFileManager);
        errors = false;

        List<String> compilerOptions;
        if (Jvm.isJava9Plus()) {
            compilerOptions = java9PlusOptions;
        } else {
            compilerOptions = java8Options;
        }

        compiler.getTask(null, fileManager, diagnostic -> {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errors = true;
                System.err.println(diagnostic);
            }
        }, compilerOptions, null, compilationUnits).call();
        Map<String, byte[]> result = fileManager.getAllBuffers();
        if (errors) {
            // compilation error, so we want to exclude this file from future compilation passes
            javaFileObjects.remove(className);
        }
        return result;
    }

    public Class loadFromJava(
            Class valueType, @NotNull ClassLoader classLoader, @NotNull String className,
            @NotNull String javaCode) throws ClassNotFoundException {
        Class clazz = null;
        Map<String, Class> loadedClasses;
        synchronized (loadedClassesMap) {
            loadedClasses = loadedClassesMap.get(classLoader);
            if (loadedClasses == null)
                loadedClassesMap.put(classLoader, loadedClasses = new LinkedHashMap<>());
            else
                clazz = loadedClasses.get(className);
        }
        if (clazz != null)
            return clazz;
        for (Map.Entry<String, byte[]> entry :
                compileFromJava(valueType, className, javaCode).entrySet()) {
            String className2 = entry.getKey();
            synchronized (loadedClassesMap) {
                if (loadedClasses.containsKey(className2))
                    continue;
            }
            byte[] bytes = entry.getValue();
            Class clazz2 = CompilerUtils.defineClass(classLoader, className2, bytes);
            synchronized (loadedClassesMap) {
                loadedClasses.put(className2, clazz2);
            }
        }
        synchronized (loadedClassesMap) {
            loadedClasses.put(className, clazz = classLoader.loadClass(className));
        }
        return clazz;
    }
}
