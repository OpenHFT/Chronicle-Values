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

import com.squareup.javapoet.*;
import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;

import static javax.lang.model.element.Modifier.*;

class ValueBuilder {

    final ValueModel model;
    final String className;
    final TypeSpec.Builder typeBuilder;
    private FieldSpec unsafe;
    private CodeBlock.Builder staticBlockBuilder;
    private MethodSpec.Builder defaultConstructorBuilder;
    private FieldSpec bytesStoreForPointers;

    public ValueBuilder(ValueModel model, String className, TypeSpec.Builder typeBuilder) {
        this.model = model;
        this.className = className;
        this.typeBuilder = typeBuilder;
    }

    ClassName className() {
        return ClassName.get(model.valueType.getPackage().getName(), className);
    }

    FieldSpec unsafe() {
        if (unsafe == null) {
            Class type = Utils.UNSAFE_CLASS;
            unsafe = FieldSpec.builder(type, "UNSAFE", PRIVATE, STATIC, FINAL).build();
            typeBuilder.addField(unsafe);

            staticBlockBuilder()
                    .beginControlFlow("try")
                    .addStatement("$T theUnsafe = $T.getField($T.class, $S)",
                            Field.class, Jvm.class, type, "theUnsafe")
                    .addStatement("$N = ($T) theUnsafe.get(null)", unsafe, type);

        }
        return unsafe;
    }

    CodeBlock.Builder staticBlockBuilder() {
        if (staticBlockBuilder == null)
            staticBlockBuilder = CodeBlock.builder();
        return staticBlockBuilder;
    }

    MethodSpec.Builder defaultConstructorBuilder() {
        if (defaultConstructorBuilder == null) {
            defaultConstructorBuilder = MethodSpec.constructorBuilder();
            defaultConstructorBuilder.addModifiers(PUBLIC);
        }
        return defaultConstructorBuilder;
    }

    void closeConstructorsAndInitializationBlocks() {
        if (staticBlockBuilder != null) {
            staticBlockBuilder.nextControlFlow("catch ($T e)", IllegalAccessException.class);
            staticBlockBuilder.addStatement("throw new $T(e)", AssertionError.class);
            staticBlockBuilder.endControlFlow();
            typeBuilder.addStaticBlock(staticBlockBuilder.build());
        }
        if (defaultConstructorBuilder != null) {
            typeBuilder.addMethod(defaultConstructorBuilder.build());
        }
    }

    FieldSpec bytesStoreForPointers() {
        if (bytesStoreForPointers == null) {
            ParameterizedTypeName bsType =
                    ParameterizedTypeName.get(NativeBytesStore.class, Void.class);
            bytesStoreForPointers = FieldSpec
                    .builder(bsType, "bytesStoreForPointers", PRIVATE, STATIC, FINAL)
                    .initializer("$T.instance()", PointersBytesStore.class)
                    .build();
            typeBuilder.addField(bytesStoreForPointers);
        }
        return bytesStoreForPointers;
    }
}
