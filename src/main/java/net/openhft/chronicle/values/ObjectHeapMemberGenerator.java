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

import com.squareup.javapoet.MethodSpec;
import sun.misc.Unsafe;

import java.util.Objects;

import static java.lang.String.format;

class ObjectHeapMemberGenerator extends HeapMemberGenerator {
    ObjectHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    @Override
    String putVolatile() {
        return "putVolatileObject";
    }

    @Override
    String putOrdered() {
        return "putOrderedObject";
    }

    @Override
    String compareAndSwap() {
        return "compareAndSwapObject";
    }

    @Override
    String arrayBase() {
        return "ARRAY_OBJECT_BASE_OFFSET";
    }

    @Override
    String arrayScale() {
        return "ARRAY_OBJECT_INDEX_SCALE";
    }

    @Override
    String wrap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String rawStoredValue) {
        return rawStoredValue;
    }

    @Override
    String unwrap(MethodSpec.Builder methodBuilder, String inputValue) {
        return inputValue;
    }

    @Override
    public void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return ($T) $N.getObjectVolatile(this, $N)",
                fieldModel.type, valueBuilder.unsafe(), fieldOffset(valueBuilder));
    }

    @Override
    public void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement("return ($T) $N.getObjectVolatile($N, " +
                        "(long) $T.ARRAY_OBJECT_BASE_OFFSET + " +
                        "(index * (long) $T.ARRAY_OBJECT_INDEX_SCALE))",
                fieldModel.type, valueBuilder.unsafe(), field, Unsafe.class, Unsafe.class);
    }

    @Override
    void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addCode("if (!$T.equals($N, other.$N())) return false;\n",
                Objects.class, field, fieldModel.getOrGetVolatile().getName());
    }

    @Override
    void generateArrayElementEquals(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addCode("if (!$T.equals($N[index], other.$N(index))) return false;\n",
                Objects.class, field, arrayFieldModel.getOrGetVolatile().getName());
    }

    @Override
    String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        return format("java.util.Objects.hashCode(%s)", field.name);
    }

    @Override
    String generateArrayElementHashCode(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        return format("java.util.Objects.hashCode(%s[index])", field.name);
    }
}
