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

import com.squareup.javapoet.*;
import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Utility responsible for generating the heap and native implementation
 * classes for a value interface. Each method produces a block of Java source
 * code to be compiled later by {@link ValueModel}. Only ASCII characters and
 * British spelling are used in generated text.
 */
final class Generators {

    private Generators() {
    }

    /**
     * Generates the source for the native (flyweight) implementation.
     * The returned string forms a complete Java class that implements
     * the given model and the {@link Byteable} contract.
     *
     * @param model            description of the value interface
     * @param nativeClassName  simple name of the class to emit
     * @return Java source code of the native class
     */
    static String generateNativeClass(ValueModel model, String nativeClassName) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(nativeClassName);
        typeBuilder.addModifiers(PUBLIC);
        ValueBuilder valueBuilder = new ValueBuilder(model, nativeClassName, typeBuilder);
        model.fields().forEach(f -> f.generateNativeMembers(valueBuilder));
        generateNativeCommons(valueBuilder);
        valueBuilder.closeConstructorsAndInitializationBlocks();
        TypeSpec nativeType = typeBuilder.build();
        String result = JavaFile
                .builder(Jvm.getPackageName(model.valueType), nativeType)
                .build()
                .toString();
        if (Jvm.getBoolean("chronicle.values.dumpCode"))
            System.out.println(result);
        return result;
    }

    /**
     * Adds fields and methods required by {@link Byteable} to the native
     * implementation and delegates to {@link #generateValueCommons(ValueBuilder, ImplType)}
     * for shared behaviour.
     */
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
                    .addStatement("    if (offset + length > bytesStore.capacity())\n" +
                            "        throw new AssertionError()")
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

    /**
     * Inserts behaviour common to both heap and native implementations. This
     * wires the generated type to the user interface, {@link Copyable} and
     * {@link BytesMarshallable}, and adds standard methods such as
     * {@code copyFrom} and marshalling helpers.
     */
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
                .addMethod(hashCodeMethod(valueBuilder, implType));
//                .addMethod(toStringMethod(valueBuilder, implType));
    }

    /**
     * Builds the {@code copyFrom} method. Each field is copied individually
     * using the relevant {@link MemberGenerator}. For native classes the method
     * optimises the case where the source is another native instance by copying
     * the underlying bytes directly.
     */
    private static MethodSpec copyFromMethod(ValueBuilder valueBuilder, ImplType implType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("copyFrom")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(valueBuilder.model.valueType, "from");
        if (implType == ImplType.NATIVE) {
            ClassName nativeClassName = valueBuilder.className();
            methodBuilder.beginControlFlow("if (from instanceof $T)", nativeClassName);
            {
                methodBuilder.addStatement(
                        "bs.write(offset, (($T) from).bytesStore(), (($T) from).offset(), $L)",
                        nativeClassName, nativeClassName, valueBuilder.model.sizeInBytes());
            }
            methodBuilder.nextControlFlow("else");
        }
        valueBuilder.model.fields()
                .forEach(f -> {
                    // plain java blocks to isolate variable namespaces
                    methodBuilder.beginControlFlow("");
                    implType.getMemberGenerator(f).generateCopyFrom(valueBuilder, methodBuilder);
                    methodBuilder.endControlFlow();
                });
        if (implType == ImplType.NATIVE) {
            methodBuilder.endControlFlow();
        }
        return methodBuilder.build();
    }

    /**
     * Constructs the method that reads the state from a {@link BytesIn}.
     * Each field delegates the read logic to its {@link MemberGenerator}.
     */
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

    /**
     * Constructs the method that writes the state to a {@link BytesOut}.
     * Delegates the actual encoding of each field to its generator.
     */
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

    /**
     * Generates the {@code equals} method. Field equality is implemented by
     * delegating to each field's generator, ensuring the semantics match the
     * interface definition.
     */
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
     * Generates the {@code hashCode} method following the approach used by
     * Google's AutoValue. Each field contributes to the hash via its
     * {@link MemberGenerator} specific logic.
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
        // check it's valid
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

    /**
     * Generates the heap backed implementation. The heap class stores field
     * values directly in normal Java objects and may implement
     * {@link HeapByteable} when the original interface extends
     * {@link Byteable}.
     *
     * @param model          description of the value interface
     * @param heapClassName  simple name of the class to emit
     * @return Java source code of the heap class
     */
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
                .builder(Jvm.getPackageName(model.valueType), heapType)
                .build()
                .toString();
        if (Jvm.getBoolean("chronicle.values.dumpCode"))
            System.out.println(result);
        return result;
    }

    /**
     * Creates a {@link MethodSpec.Builder} mirroring the given reflective
     * method. Parameter names are supplied externally as reflection does not
     * retain them prior to Java 8 compilation with debugging information.
     */
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
}
