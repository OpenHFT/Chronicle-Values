/*
 *      Copyright (C) 2015, 2016-2020 chronicle.software
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
import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.core.Jvm;
import sun.misc.Unsafe;

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
            unsafe = FieldSpec.builder(Unsafe.class, "UNSAFE", PRIVATE, STATIC, FINAL).build();
            typeBuilder.addField(unsafe);

            staticBlockBuilder()
                    .beginControlFlow("try")
                    .addStatement("$T theUnsafe = $T.getField($T.class, $S)",
                            Field.class, Jvm.class, Unsafe.class, "theUnsafe")
                    .addStatement("$N = ($T) theUnsafe.get(null)", unsafe, Unsafe.class);
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
