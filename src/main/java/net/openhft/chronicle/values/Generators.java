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

import com.squareup.javapoet.*;
import net.openhft.chronicle.bytes.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

final class Generators {

    private Generators() {
    }

    static String generateNativeClass(ValueModel model, String nativeClassName) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(nativeClassName);
        typeBuilder.addModifiers(PUBLIC);
        ValueBuilder valueBuilder = new ValueBuilder(model, nativeClassName, typeBuilder);
        model.fields().forEach(f -> f.generateNativeMembers(valueBuilder));
        generateNativeCommons(valueBuilder);
        valueBuilder.closeConstructorsAndInitializationBlocks();
        TypeSpec nativeType = typeBuilder.build();
        String result = JavaFile
                .builder(model.valueType.getPackage().getName(), nativeType)
                .build()
                .toString();
        if (Boolean.getBoolean("chronicle.values.dumpCode"))
            System.out.println(result);
        return result;
    }

    private static void generateNativeCommons(ValueBuilder valueBuilder) {
        generateValueCommons(valueBuilder, ImplType.NATIVE);
        ValueModel model = valueBuilder.model;
        valueBuilder.typeBuilder
                .addSuperinterface(Byteable.class)
                .addField(BytesStore.class, "bs", PRIVATE)
                .addField(long.class, "offset", PRIVATE)
                .addMethod(bytesStoreMethod(model))
                .addMethod(bytesStoreGetterMethod())
                .addMethod(offsetMethod())
                .addMethod(maxSizeMethod(model));
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

    private enum ImplType {
        HEAP {
            @Override
            MemberGenerator getMemberGenerator(FieldModel fieldModel) {
                return fieldModel.heapGenerator();
            }
        },
        NATIVE {
            @Override
            MemberGenerator getMemberGenerator(FieldModel fieldModel) {
                return fieldModel.nativeGenerator();
            }
        };

        abstract MemberGenerator getMemberGenerator(FieldModel fieldModel);
    }

    private static void generateValueCommons(ValueBuilder valueBuilder, ImplType implType) {
        Class<?> valueType = valueBuilder.model.valueType;
        valueBuilder.typeBuilder
                .addSuperinterface(valueType)
                .addSuperinterface(ParameterizedTypeName.get(Copyable.class, valueType))
                .addSuperinterface(BytesMarshallable.class);
        valueBuilder.typeBuilder
                .addMethod(copyFromMethod(valueBuilder, implType))
                .addMethod(writeMarshallableMethod(valueBuilder, implType))
                .addMethod(readMarshallableMethod(valueBuilder, implType))
                .addMethod(equalsMethod(valueBuilder, implType))
                .addMethod(hashCodeMethod(valueBuilder, implType))
                .addMethod(toStringMethod(valueBuilder, implType));
    }

    private static MethodSpec copyFromMethod(ValueBuilder valueBuilder, ImplType implType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("copyFrom")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(valueBuilder.model.valueType, "from");
        valueBuilder.model.fields()
                .forEach(f -> {
                    // plain java blocks to isolate variable namespaces
                    methodBuilder.beginControlFlow("");
                    implType.getMemberGenerator(f).generateCopyFrom(valueBuilder, methodBuilder);
                    methodBuilder.endControlFlow();
                });
        return methodBuilder.build();
    }

    private static MethodSpec readMarshallableMethod(ValueBuilder valueBuilder, ImplType implType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("readMarshallable")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(BytesIn.class, "bytes");
        valueBuilder.model.fields()
                .forEach(f -> {
                    // plain java blocks to isolate variable namespaces
                    methodBuilder.beginControlFlow("");
                    implType.getMemberGenerator(f)
                            .generateReadMarshallable(valueBuilder, methodBuilder);
                    methodBuilder.endControlFlow();
                });
        return methodBuilder.build();
    }

    private static MethodSpec writeMarshallableMethod(
            ValueBuilder valueBuilder, ImplType implType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("writeMarshallable")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(BytesOut.class, "bytes");
        valueBuilder.model.fields()
                .forEach(f -> {
                    // plain java blocks to isolate variable namespaces
                    methodBuilder.beginControlFlow("");
                    implType.getMemberGenerator(f)
                            .generateWriteMarshallable(valueBuilder, methodBuilder);
                    methodBuilder.endControlFlow();
                });
        return methodBuilder.build();
    }

    private static MethodSpec equalsMethod(ValueBuilder valueBuilder, ImplType implType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("equals")
                .addParameter(Object.class, "obj")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(boolean.class);
        Class<?> valueType = valueBuilder.model.valueType;
        methodBuilder.addCode("if (!(obj instanceof $T)) return false;\n",
                valueType);
        methodBuilder.addStatement("$T other = ($T) obj", valueType, valueType);
        valueBuilder.model.fields().forEach(f -> {
            // plain java blocks to isolate variable namespaces
            methodBuilder.beginControlFlow("");
            implType.getMemberGenerator(f).generateEquals(valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
        });
        methodBuilder.addStatement("return true");
        return methodBuilder.build();
    }

    /**
     * Copies google/auto value's strategy of hash code generation
     */
    private static MethodSpec hashCodeMethod(ValueBuilder valueBuilder, ImplType implType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(int.class);
        methodBuilder.addStatement("int hashCode = 1");
        valueBuilder.model.fields().forEach(f -> {
            methodBuilder.addStatement("hashCode *= 1000003");
            // plain java blocks to isolate variable namespaces
            methodBuilder.beginControlFlow("");
            String fieldHashCode =
                    implType.getMemberGenerator(f).generateHashCode(valueBuilder, methodBuilder);
            methodBuilder.addStatement("hashCode ^= $N", fieldHashCode);
            methodBuilder.endControlFlow();
        });
        methodBuilder.addStatement("return hashCode");
        return methodBuilder.build();
    }

    private static MethodSpec toStringMethod(ValueBuilder valueBuilder, ImplType implType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(String.class);
        methodBuilder.addStatement("$T sb = new $T()", StringBuilder.class, StringBuilder.class);
        String modelName = valueBuilder.model.simpleName();
        methodBuilder.addStatement("sb.append($S)", modelName);
        valueBuilder.model.fields().forEach(f -> {
            // plain java blocks to isolate variable namespaces
            methodBuilder.beginControlFlow("");
            implType.getMemberGenerator(f).generateToString(valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
        });
        methodBuilder.addStatement("sb.setCharAt($L, '{')", modelName.length());
        methodBuilder.addStatement("sb.append(' ').append('}')");
        methodBuilder.addStatement("return sb.toString()");
        return methodBuilder.build();
    }

    static String generateHeapClass(ValueModel model, String heapClassName) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(heapClassName);
        typeBuilder.addModifiers(PUBLIC);
        ValueBuilder valueBuilder = new ValueBuilder(model, heapClassName, typeBuilder);
        model.fields().forEach(f -> f.generateHeapMembers(valueBuilder));
        generateValueCommons(valueBuilder, ImplType.HEAP);
        if (Byteable.class.isAssignableFrom(model.valueType))
            typeBuilder.addSuperinterface(HeapByteable.class);
        valueBuilder.closeConstructorsAndInitializationBlocks();
        TypeSpec heapType = typeBuilder.build();
        String result = JavaFile
                .builder(model.valueType.getPackage().getName(), heapType)
                .build()
                .toString();
        if (Boolean.getBoolean("chronicle.values.dumpCode"))
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
}
