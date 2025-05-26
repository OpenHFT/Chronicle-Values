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
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;

import static javax.lang.model.element.Modifier.*;

/**
 * Helper that assembles the pieces of a generated implementation during code
 * generation. A {@code ValueBuilder} instance holds onto the {@link ValueModel}
 * describing the source interface, the simple name of the class being
 * generated and the Pojo builder used to emit its bytecode.
 */
class ValueBuilder {

    /** metadata of the value interface being implemented */
    final ValueModel model;
    /** simple name of the generated class */
    final String className;
    /** builder for the generated type */
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
        return ClassName.get(Jvm.getPackageName(model.valueType), className);
    }

    /**
     * Returns the field modelling {@code sun.misc.Unsafe}. On first call the
     * field is declared and a static block is prepared to obtain the instance
     * reflectively from {@code Jvm.theUnsafe}.
     */
    FieldSpec unsafe() {
        if (unsafe == null) {
            Class<?> type = Utils.UNSAFE_CLASS;
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

    /**
     * Lazily creates the builder for the class' static initialisation block.
     * Used by {@link #unsafe()} and by field generators.
     */
    CodeBlock.Builder staticBlockBuilder() {
        if (staticBlockBuilder == null)
            staticBlockBuilder = CodeBlock.builder();
        return staticBlockBuilder;
    }

    /**
     * Lazily creates the builder for the default public constructor so that
     * field generators may append initialisation logic.
     */
    MethodSpec.Builder defaultConstructorBuilder() {
        if (defaultConstructorBuilder == null) {
            defaultConstructorBuilder = MethodSpec.constructorBuilder();
            defaultConstructorBuilder.addModifiers(PUBLIC);
        }
        return defaultConstructorBuilder;
    }

    /**
     * Finalises any open initialisation blocks and constructors and adds them
     * to the generated type.
     */
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

    /**
     * Shared {@link PointerBytesStore} used when pointer fields need a
     * temporary BytesStore. Added once to the generated type.
     */
    FieldSpec bytesStoreForPointers() {
        if (bytesStoreForPointers == null) {
            bytesStoreForPointers = FieldSpec
                    .builder(TypeName.get(PointerBytesStore.class), "bytesStoreForPointers", PRIVATE, FINAL)
                    .initializer("new $T()", PointerBytesStore.class)
                    .build();
            typeBuilder.addField(bytesStoreForPointers);
        }
        return bytesStoreForPointers;
    }
}
