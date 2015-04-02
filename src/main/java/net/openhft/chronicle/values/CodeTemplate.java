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

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public interface CodeTemplate {
    static CodeTemplate heap() {
        return VanillaCodeTemplate.of(c -> c.getName() + "$$Heap")
                .addMethodPattern("get(\\w+)", 0, Method::getReturnType, CodeTemplate::getter, CodeTemplate::jcGetter, CodeTemplate::bcGetter)
                .addMethodPattern("is(\\w+)", 0, Method::getReturnType, CodeTemplate::getter, CodeTemplate::jcGetter, CodeTemplate::bcGetter)
                .addMethodPattern("set(\\w+)", 1, m -> m.getParameterTypes()[0], CodeTemplate::setter, CodeTemplate::jcSetter, CodeTemplate::bcSetter)
                .addMethodPattern("add(\\w+)", 1, m -> m.getParameterTypes()[0], CodeTemplate::adder, CodeTemplate::jcAdder, CodeTemplate::bcAdder)
                .addMethodPattern("addAtomic(\\w+)", 1, m -> m.getParameterTypes()[0], CodeTemplate::addAtomic, CodeTemplate::jcAddAtomic, CodeTemplate::bcAddAtomic)
                ;
    }

    static void bcAddAtomic(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcAddAtomic(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void addAtomic(Method method, FieldModel fieldModel) {
        throw new UnsupportedOperationException();
    }

    static void bcAdder(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcAdder(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void adder(Method method, FieldModel fieldModel) {
        throw new UnsupportedOperationException();
    }

    static void bcSetter(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcSetter(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void bcGetter(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcGetter(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void getter(Method method, FieldModel fieldModel) {
        fieldModel.setGetter(method);
    }

    static void setter(Method method, FieldModel fieldModel) {
        fieldModel.setSetter(method);
    }

    static CodeTemplate direct() {
        return VanillaCodeTemplate.of(c -> c.getName() + "$$Native")
                .addMethodPattern("get(\\w+)", 0, Method::getReturnType, CodeTemplate::getter, CodeTemplate::jcNativeGetter, CodeTemplate::bcNativeGetter)
                .addMethodPattern("is(\\w+)", 0, Method::getReturnType, CodeTemplate::getter, CodeTemplate::jcNativeGetter, CodeTemplate::bcNativeGetter)
                .addMethodPattern("set(\\w+)", 1, m -> m.getParameterTypes()[0], CodeTemplate::setter, CodeTemplate::jcNativeSetter, CodeTemplate::bcNativeSetter)
                .addMethodPattern("add(\\w+)", 1, m -> m.getParameterTypes()[0], CodeTemplate::adder, CodeTemplate::jcNativeAdder, CodeTemplate::bcNativeAdder)
                .addMethodPattern("addAtomic(\\w+)", 1, m -> m.getParameterTypes()[0], CodeTemplate::addAtomic, CodeTemplate::jcNativeAddAtomic, CodeTemplate::bcNativeAddAtomic)
                .addFieldInspector(CodeTemplate::sortFieldsById);
    }

    static void sortFieldsById(LinkedHashMap<String, FieldModel> fieldModelMap) {
        List<Map.Entry<String, FieldModel>> sortedFields = fieldModelMap.entrySet().stream()
                .sorted(comparing((Map.Entry<String, FieldModel> e) -> e.getValue().getGroupId())
                        .thenComparing((Map.Entry<String, FieldModel> e) -> -e.getValue().byteAlignment()))
                .collect(Collectors.toList());
        fieldModelMap.clear();
        sortedFields.forEach(e -> fieldModelMap.put(e.getKey(), e.getValue()));
    }

    static void bcNativeAddAtomic(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcNativeAddAtomic(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void bcNativeAdder(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcNativeAdder(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void bcNativeSetter(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcNativeSetter(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void bcNativeGetter(FieldModel fieldModel, ByteCodeModel byteCodeModel) {
        throw new UnsupportedOperationException();
    }

    static void jcNativeGetter(FieldModel fieldModel, JavaCodeModel javaCodeModel) {
        throw new UnsupportedOperationException();
    }

    CodeTemplate addFieldInspector(Consumer<LinkedHashMap<String, FieldModel>> fieldInspector);

    CodeTemplate addMethodPattern(String regex, int arguments,
                                  Function<Method, Class> fieldType, BiConsumer<Method, FieldModel> templateExtractor,
                                  BiConsumer<FieldModel, JavaCodeModel> javaCodeGenerator,
                                  BiConsumer<FieldModel, ByteCodeModel> byteCodeGenerator);

    CodeTemplate generateJava(boolean generateJava);

    <T> T newInstance(Class<T> tClass);
}
