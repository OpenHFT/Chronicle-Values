/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;


import net.openhft.chronicle.core.ClassLoading;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public class VanillaCodeTemplate implements CodeTemplate {
    final List<Consumer<LinkedHashMap<String, FieldModel>>> fieldInspectors = new ArrayList<>();
    final SortedMap<MethodKey, MethodTemplates> methodPatterns = new TreeMap<>(Comparator
            .comparing((MethodKey k) -> k.arguments)
            .thenComparing(k -> -k.regex.length())
            .thenComparing(k -> k.regex));

    private final Function<Class, String> nameForClass;
    private boolean generateJava = false;

    public VanillaCodeTemplate(Function<Class, String> nameForClass) {
        this.nameForClass = nameForClass;
    }

    public static CodeTemplate of(Function<Class, String> nameForClass) {
        return new VanillaCodeTemplate(nameForClass);
    }

    @Override
    public CodeTemplate addFieldInspector(Consumer<LinkedHashMap<String, FieldModel>> fieldInspector) {
        fieldInspectors.add(fieldInspector);
        return this;
    }

    @Override
    public CodeTemplate addMethodPattern(String regex, int arguments,
                                         Function<Method, Class> fieldType, BiConsumer<Method, FieldModel> templateExtractor,
                                         BiConsumer<FieldModel, JavaCodeModel> javaCodeGenerator,
                                         BiConsumer<FieldModel, ByteCodeModel> byteCodeGenerator) {
        methodPatterns.put(new MethodKey(regex, arguments),
                new MethodTemplates(fieldType, templateExtractor, javaCodeGenerator, byteCodeGenerator));
        return this;
    }

    @Override
    public CodeTemplate generateJava(boolean generateJava) {
        this.generateJava = generateJava;
        return this;
    }

    @Override
    public <T> T newInstance(Class<T> tClass) {
        String name = nameForClass.apply(tClass);
        try {
            return (T) Class.forName(name).newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (ClassNotFoundException e) {
            // not found, needs to generate
        }
        // build up the field models.
        LinkedHashMap<String, FieldModel> fieldModelMap = new LinkedHashMap<>();
        Stream.of(tClass.getDeclaredMethods())
                .filter(m -> (m.getModifiers() & Modifier.ABSTRACT) != 0)
                .forEach(m -> {
                    Map.Entry<MethodKey, MethodTemplates> entry = methodPatterns.entrySet().stream()
                            .filter(e -> e.getKey().arguments == m.getParameterCount())
                            .filter(e -> m.getName().matches(e.getKey().regex))
                            .findFirst().orElseThrow(IllegalStateException::new);
                    Matcher matcher = Pattern.compile(entry.getKey().regex).matcher(m.getName());
                    if (!matcher.find()) throw new AssertionError();
                    String fieldName = convertFieldName(matcher.group(1));
                    FieldModel fieldModel = fieldModelMap.computeIfAbsent(fieldName, VanillaFieldModel::new);
                    entry.getValue().templateExtractor.accept(m, fieldModel);
                    fieldModel.addTemplate(m, entry.getKey(), entry.getValue());
                });
        fieldInspectors.forEach(fi -> fi.accept(fieldModelMap));
        JavaCodeModel jcm = VanillaJavaCodeModel.forName(name, tClass);
        ByteCodeModel bcm = VanillaByteCodeModel.forName(name, tClass);

        fieldModelMap.entrySet().forEach(e -> {
                    jcm.addField(e.getKey(), e.getValue());
                    bcm.addField(e.getKey(), e.getValue());
                }
        );
        System.out.println(jcm.generateJavaCode());
        try {
            return (T) ClassLoading.defineClass(name, bcm.generateByteCode()).newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    protected String convertFieldName(String name) {
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) return name;
        if (Character.isLowerCase(name.charAt(0))) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
