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

import com.squareup.javapoet.*;
import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

final class Generators {

    private static final boolean dumpCode = Boolean.getBoolean("chronicle.values.dumpCode");

    static String generateNativeClass(ValueModel model, String nativeClassName) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(nativeClassName);
        typeBuilder.addModifiers(PUBLIC);
        ValueBuilder valueBuilder = new ValueBuilder(model, nativeClassName, typeBuilder);
        model.fields().forEach(f -> f.generateNativeMembers(valueBuilder));
        generateNativeCommons(valueBuilder);
        TypeSpec nativeType = typeBuilder.build();
        String result = JavaFile
                .builder(model.valueType.getPackage().getName(), nativeType)
                .build()
                .toString();
        if (dumpCode)
            System.out.println(result);
        return result;
    }

    private static void generateNativeCommons(ValueBuilder valueBuilder) {
        generateValueCommons(valueBuilder);
        ValueModel model = valueBuilder.model;
        valueBuilder.typeBuilder
                .addSuperinterface(Byteable.class)
                .addField(BytesStore.class, "bs", PRIVATE)
                .addField(long.class, "offset", PRIVATE)
                .addMethod(bytesStoreMethod(model))
                .addMethod(bytesStoreGetterMethod())
                .addMethod(offsetMethod())
                .addMethod(maxSizeMethod(model))
                .addMethod(copyFromMethod(valueBuilder, FieldModel::nativeGenerator));
    }

    private static MethodSpec bytesStoreMethod(ValueModel model) {
        try {
            Method bytesStoreReflectMethod = Byteable.class
                    .getMethod("bytesStore", BytesStore.class, long.class, long.class);
            return methodBuilder(bytesStoreReflectMethod, asList("bytesStore", "offset", "length"))
                    .beginControlFlow("if (length != maxSize())")
                    .addStatement("throw new $T($S + length)",
                            IllegalArgumentException.class,
                            format("Constant size is %d, given length is ", model.sizeInBytes()))
                    .endControlFlow()
                    .addStatement("this.bs = bytesStore")
                    .addStatement("this.offset = offset")
                    .build();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static MethodSpec bytesStoreGetterMethod() {
        try {
            Method bytesStoreReflectMethod = Byteable.class.getMethod("bytesStore");
            return methodBuilder(bytesStoreReflectMethod, emptyList())
                    .addStatement("return bs")
                    .build();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static MethodSpec offsetMethod() {
        try {
            return methodBuilder(Byteable.class.getMethod("offset"), emptyList())
                    .addStatement("return offset")
                    .build();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static MethodSpec maxSizeMethod(ValueModel model) {
        try {
            return methodBuilder(Byteable.class.getMethod("maxSize"), emptyList())
                    .addStatement("return $L", model.sizeInBytes())
                    .build();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static void generateValueCommons(ValueBuilder valueBuilder) {
        Class<?> valueType = valueBuilder.model.valueType;
        valueBuilder.typeBuilder
                .addSuperinterface(valueType)
                .addSuperinterface(ParameterizedTypeName.get(Copyable.class, valueType));
    }

    private static MethodSpec copyFromMethod(
            ValueBuilder valueBuilder, Function<FieldModel, MemberGenerator> generator) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("copyFrom")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(valueBuilder.model.valueType, "from");
        valueBuilder.model.fields()
                .forEach(f -> {
                    methodBuilder.addCode("// Copy $N field\n", f.name);
                    generator.apply(f).generateCopyFrom(valueBuilder, methodBuilder);
                });
        return methodBuilder.build();
    }

    static String generateHeapClass(ValueModel model, String heapClassName) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(heapClassName);
        typeBuilder.addModifiers(PUBLIC);
        ValueBuilder valueBuilder = new ValueBuilder(model, heapClassName, typeBuilder);
        model.fields().forEach(f -> f.generateHeapMembers(valueBuilder));
        generateValueCommons(valueBuilder);
        valueBuilder.typeBuilder
                .addMethod(copyFromMethod(valueBuilder, FieldModel::heapGenerator));
        TypeSpec heapType = typeBuilder.build();
        String result = JavaFile
                .builder(model.valueType.getPackage().getName(), heapType)
                .build()
                .toString();
        if (dumpCode)
            System.out.println(result);
        return result;
    }

    static MethodSpec.Builder methodBuilder(Method m, List<String> paramNames) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(m.getName())
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC);
        builder.returns(m.getReturnType());
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            builder.addParameter(ParameterSpec.builder(p.getType(), paramNames.get(i)).build());
        }
        return builder;
    }

    private Generators() {}
}
