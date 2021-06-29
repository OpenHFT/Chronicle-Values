/*
 * Copyright 2016-2021 chronicle.software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class support loading and debugging Java Classes dynamically.
 */
enum CompilerUtils {
    ; // none
    public static final CachedCompiler CACHED_COMPILER = new CachedCompiler();

    private static final Method DEFINE_CLASS_METHOD;
    static JavaCompiler s_compiler;
    static StandardJavaFileManager s_standardJavaFileManager;

    static {
        try {
            DEFINE_CLASS_METHOD = ClassLoader.class.getDeclaredMethod(
                    "defineClass", String.class, byte[].class, int.class, int.class);
            Jvm.setAccessible(DEFINE_CLASS_METHOD);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    static {
        reset();
    }

    private static void reset() {
        s_compiler = ToolProvider.getSystemJavaCompiler();
        if (s_compiler == null) {
            try {
                Class<?> javacTool = Class.forName("com.sun.tools.javac.api.JavacTool");
                Method create = javacTool.getMethod("create");
                s_compiler = (JavaCompiler) create.invoke(null);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        s_standardJavaFileManager = s_compiler.getStandardFileManager(null, null, null);
    }

    /**
     * Define a class for byte code.
     *
     * @param classLoader to load the class into.
     * @param className   expected to load.
     * @param bytes       of the byte code.
     */
    public static Class defineClass(
            @Nullable ClassLoader classLoader, @NotNull String className, @NotNull byte[] bytes) {
        try {
            return (Class) DEFINE_CLASS_METHOD
                    .invoke(classLoader, className, bytes, 0, bytes.length);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new AssertionError(e.getCause());
        }
    }
}
