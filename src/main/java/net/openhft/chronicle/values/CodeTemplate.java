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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesMarshallable;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.values.MethodTemplate.Type.ARRAY;
import static net.openhft.chronicle.values.MethodTemplate.Type.SCALAR;

final class CodeTemplate {

    public static final Function<Method, Parameter> NO_ANNOTATED_PARAM = m -> null;

    private static final SortedSet<MethodTemplate> METHOD_TEMPLATES =
            new TreeSet<>(
                    comparing((MethodTemplate t) -> t.parameters)
                            .thenComparing(k -> -k.regex.length())
                            .thenComparing(k -> k.regex));

    static {
        addReadPatterns("get", 0, FieldModel::setGet);
        addReadPatterns("", 0, FieldModel::setGet);
        addReadPatterns("is", 0, FieldModel::setGet);
        addReadPatterns("getVolatile", 0, FieldModel::setGetVolatile);
        addReadPatterns("getUsing", 1, FieldModel::setGetUsing);
        addWritePattern("set", 1, FieldModel::setSet);
        addWritePattern("", 1, FieldModel::setSet);
        addWritePattern("setVolatile", 1, FieldModel::setSetVolatile);
        addWritePattern("setOrdered", 1, FieldModel::setSetOrdered);
        addWritePattern("add", 1, FieldModel::setAdd);
        addWritePattern("addAtomic", 1, FieldModel::setAddAtomic);
        addWritePattern("compareAndSwap", 2, FieldModel::setCompareAndSwap);
    }

    private static final String FIELD_NAME = "([a-zA-Z$][a-zA-Z\\d_$]*)";

    private static void addReadPatterns(
            String regex, int arguments, BiConsumer<FieldModel, Method> addMethodToModel) {
        regex += FIELD_NAME;
        add(regex, arguments, SCALAR, Method::getReturnType, NO_ANNOTATED_PARAM, addMethodToModel);
        add(regex + "At", arguments + 1, ARRAY, Method::getReturnType, NO_ANNOTATED_PARAM,
                addMethodToModel);
    }

    public static void addWritePattern(
            String regex, int arguments, BiConsumer<FieldModel, Method> addMethodToModel) {
        regex += FIELD_NAME;
        add(regex, arguments, SCALAR,
                m -> m.getParameterTypes()[arguments - 1],
                m -> m.getParameters()[arguments - 1],
                addMethodToModel);
        add(regex + "At", arguments + 1, ARRAY,
                m -> m.getParameterTypes()[arguments],
                m -> m.getParameters()[arguments],
                addMethodToModel);
    }

    private static void add(
            String regex, int parameters, MethodTemplate.Type type,
            Function<Method, Class> fieldType, Function<Method, Parameter> annotatedParameter,
            BiConsumer<FieldModel, Method> addMethodToModel) {
        METHOD_TEMPLATES.add(new MethodTemplate(regex, parameters, type, fieldType,
                annotatedParameter, addMethodToModel));
    }

    static ValueModel createValueModel(Class<?> valueType) {
        // build up the field models.
        LinkedHashMap<String, FieldModel> fieldModelMap = new LinkedHashMap<>();
        forEachAbstractMethod(valueType, m -> {
            MethodTemplate methodTemplate = METHOD_TEMPLATES.stream()
                    .filter(t -> t.parameters == m.getParameterCount())
                    .filter(t -> m.getName().matches(t.regex))
                    .findFirst().orElseThrow(IllegalStateException::new);
            Matcher matcher = Pattern.compile(methodTemplate.regex).matcher(m.getName());
            if (!matcher.find())
                throw new AssertionError();
            String fieldName = convertFieldName(matcher.group(1));
            FieldModel fieldModel = fieldModelMap.computeIfAbsent(fieldName,
                    n -> {
                        FieldModel model = methodTemplate.createModel(m, n);
                        model.name = fieldName;
                        return model;
                    });
            methodTemplate.addMethodToModel.accept(fieldModel, m);
            fieldModel.addInfo(m, methodTemplate);
        });
        List<FieldModel> fields = fieldModelMap.values().stream().collect(toList());
        fields.forEach(FieldModel::postProcess);
        fields.forEach(FieldModel::checkState);
        return new ValueModel(valueType, fields.stream());
    }

    private static void forEachAbstractMethod(Class<?> c, Consumer<Method> action) {
        Stream.of(c.getMethods())
                .filter(m -> (m.getModifiers() & Modifier.ABSTRACT) != 0)
                .filter(m -> NON_MODEL_TYPES.stream().noneMatch(t -> hasMethod(t, m)))
                // sorts methods in the order of ascending name lengths
                // this forEachAbstractMethod() is called in createValueModel(), where FieldModel
                // are created lazily from the field type info appearing in the first processed
                // method. Char Sequence fields could have a method void getUsing() which doesn't
                // contain actual field type info (String or CharSequence).
                // When we sort methods, getFoo() or setFoo() is always processed before potential
                // getUsing(), and the field could be created lazily like in general case
                .sorted(comparing(m -> m.getName().length()))
                .forEach(action);
    }

    static final List<Class<?>> NON_MODEL_TYPES = asList(
            Object.class, Serializable.class, Externalizable.class, BytesMarshallable.class,
            Copyable.class, Byteable.class);

    private static boolean hasMethod(Class<?> type, Method m) {
        return Stream.of(type.getMethods())
                .anyMatch(m2 -> m2.getName().equals(m.getName()) &&
                        Arrays.equals(m2.getParameterTypes(), m.getParameterTypes()));
    }

    static String convertFieldName(String name) {
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) return name;
        if (Character.isLowerCase(name.charAt(0))) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
