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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesMarshallable;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.values.MethodTemplate.Type.ARRAY;
import static net.openhft.chronicle.values.MethodTemplate.Type.SCALAR;
import static net.openhft.chronicle.values.Primitives.isPrimitiveIntegerType;

enum CodeTemplate {
    ;
	
    public static final Function<Method, Parameter> NO_ANNOTATED_PARAM = m -> null;
    static final List<Class<?>> NON_MODEL_TYPES = asList(
            Object.class, Serializable.class, Externalizable.class, BytesMarshallable.class,
            Copyable.class, Byteable.class);

    private static final SortedSet<MethodTemplate> METHOD_TEMPLATES =
            new TreeSet<>(
                    comparing((MethodTemplate t) -> t.parameters)
                            .thenComparing(k -> -k.regex.length())
                            .thenComparing(k -> k.regex));
    private static final String FIELD_NAME = "([a-zA-Z_$][a-zA-Z\\d_$]*)";

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
        List<FieldModel> fields = methodsAndTemplatesByField(valueType).entrySet().stream()
                .map(e -> createAndConfigureModel(e.getKey(), e.getValue())).collect(toList());
        if (fields.isEmpty())
            throw new IllegalArgumentException(valueType + " is not a value interface");
        fields.forEach(FieldModel::checkAnyWriteMethodPresent);
        fields.forEach(FieldModel::postProcess);
        fields.forEach(FieldModel::checkState);
        return new ValueModel(valueType, fields.stream());
    }

    private static FieldModel createAndConfigureModel(
            String fieldName, List<MethodAndTemplate> methodsAndTemplates) {
        if (methodsAndTemplates.stream().map(mt -> mt.template.type).distinct().count() > 1) {
            throw new IllegalArgumentException("All or none accessors of the " + fieldName +
                    " field should end with -At (what means this is an array field)");
        }
        ScalarFieldModel scalarModel =
                createAndConfigureScalarModel(fieldName, methodsAndTemplates);
        if (methodsAndTemplates.get(0).template.type == SCALAR) {
            return scalarModel;
        } else {
            ArrayFieldModel arrayModel = new ArrayFieldModel(scalarModel);
            configureModel(arrayModel, methodsAndTemplates);
            return arrayModel;
        }
    }

    private static ScalarFieldModel createAndConfigureScalarModel(
            String fieldName, List<MethodAndTemplate> methodsAndTemplates) {
        ScalarFieldModel nonPointerModel =
                createNonPointerScalarModel(fieldName, methodsAndTemplates);
        configureModel(nonPointerModel, methodsAndTemplates);

        boolean hasPointerAnnotation = methodsAndTemplates.stream().map(mt -> mt.method)
                .flatMap(m -> Arrays.stream(m.getParameterAnnotations()).flatMap(Arrays::stream))
                .anyMatch(a -> a.annotationType() == Pointer.class);
        if (hasPointerAnnotation) {
            if (!(nonPointerModel instanceof ValueFieldModel)) {
                throw new IllegalStateException(fieldName + " annotated with @Pointer but has " +
                        nonPointerModel.type.getName() + " type which is not a value interface");
            }
            PointerFieldModel pointerModel =
                    new PointerFieldModel((ValueFieldModel) nonPointerModel);
            configureModel(pointerModel, methodsAndTemplates);
            return pointerModel;
        } else {
            return nonPointerModel;
        }
    }

    private static void configureModel(
            FieldModel model, List<MethodAndTemplate> methodsAndTemplates) {
        methodsAndTemplates.forEach(mt -> {
            model.name = mt.fieldName;
            model.addInfo(mt.method, mt.template);
            mt.template.addMethodToModel.accept(model, mt.method);
        });
    }

    private static ScalarFieldModel createNonPointerScalarModel(
            String fieldName, List<MethodAndTemplate> methodsAndTemplates) {
        // CharSequence fields could have a method void getUsing() which doesn't contain actual
        // field type info (String or CharSequence).
        MethodAndTemplate nonGetUsingMethodAndTemplate = methodsAndTemplates.stream()
                .filter(mt -> !mt.template.regex.startsWith("getUsing"))
                .findAny().orElseThrow(() -> new IllegalStateException(fieldName +
                        " field should have some accessor methods except " +
                        methodsAndTemplates.get(0).method.getName()));
        MethodTemplate nonGetUsingMethodTemplate = nonGetUsingMethodAndTemplate.template;
        Method nonGetUsingMethod = nonGetUsingMethodAndTemplate.method;
        Class fieldType = nonGetUsingMethodTemplate.fieldType.apply(nonGetUsingMethod);
        if (isPrimitiveIntegerType(fieldType))
            return new IntegerFieldModel();
        if (fieldType == float.class || fieldType == double.class)
            return new FloatingFieldModel();
        if (fieldType == boolean.class)
            return new BooleanFieldModel();
        if (Enum.class.isAssignableFrom(fieldType))
            return new EnumFieldModel();
        if (fieldType == Date.class)
            return new DateFieldModel();
        if (CharSequence.class.isAssignableFrom(fieldType))
            return new CharSequenceFieldModel();
        if (fieldType.isInterface())
            return new ValueFieldModel();
        throw new IllegalStateException(fieldName + " field type " + fieldType +
                " is not supported: not a primitive, enum, CharSequence " +
                "or another value interface");
    }

    static class MethodAndTemplate {
        final Method method;
        final MethodTemplate template;
        final String fieldName;

        MethodAndTemplate(Method method, MethodTemplate template, String fieldName) {
            this.method = method;
            this.template = template;
            this.fieldName = fieldName;
        }
    }

    private static Map<String, List<MethodAndTemplate>> methodsAndTemplatesByField(
            Class<?> valueType) {
        return Stream.of(valueType.getMethods())
                    .filter(m -> (m.getModifiers() & Modifier.ABSTRACT) != 0)
                    .filter(m -> NON_MODEL_TYPES.stream().noneMatch(t -> hasMethod(t, m)))
                    .map(m -> {
                        MethodTemplate methodTemplate = METHOD_TEMPLATES.stream()
                                .filter(t -> t.parameters == m.getParameterCount())
                                .filter(t -> m.getName().matches(t.regex))
                                .findFirst().orElseThrow(IllegalStateException::new);
                        Matcher matcher = Pattern.compile(methodTemplate.regex)
                                .matcher(m.getName());
                        if (!matcher.find())
                            throw new AssertionError();
                        String fieldName = convertFieldName(matcher.group(1));
                        return new MethodAndTemplate(m, methodTemplate, fieldName);
                    }).collect(groupingBy(mt -> mt.fieldName));
    }

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
