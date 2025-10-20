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

import com.squareup.javapoet.MethodSpec;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Generates accessor logic for object fields held in heap based values.
 * The code relies on {@link sun.misc.Unsafe} for volatile, ordered and
 * compare-and-swap operations.
 */
class ObjectHeapMemberGenerator extends HeapMemberGenerator {
    ObjectHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    /**
     * Name of the Unsafe routine used for volatile writes.
     */
    @Override
    String putVolatile() {
        return "putVolatileObject";
    }

    /**
     * Name of the Unsafe routine used for ordered writes.
     */
    @Override
    String putOrdered() {
        return "putOrderedObject";
    }

    /**
     * Name of the Unsafe compare-and-swap routine for object references.
     */
    @Override
    String compareAndSwap() {
        return "compareAndSwapObject";
    }

    /**
     * Constant holding the base offset for object arrays.
     */
    @Override
    String arrayBase() {
        return "ARRAY_OBJECT_BASE_OFFSET";
    }

    /**
     * Constant holding the index scale for object arrays.
     */
    @Override
    String arrayScale() {
        return "ARRAY_OBJECT_INDEX_SCALE";
    }

    /**
     * Heap objects do not require wrapping so the stored value is used as is.
     */
    @Override
    String wrap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String rawStoredValue) {
        return rawStoredValue;
    }

    /**
     * No unwrapping is needed for heap objects.
     */
    @Override
    String unwrap(MethodSpec.Builder methodBuilder, String inputValue) {
        return inputValue;
    }

    /**
     * Emits a volatile read using {@code getObjectVolatile}.
     */
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
        Class<?> type = Utils.UNSAFE_CLASS;
        methodBuilder.addStatement("return ($T) $N.getObjectVolatile($N, " +
                        "(long) $T.ARRAY_OBJECT_BASE_OFFSET + " +
                        "(index * (long) $T.ARRAY_OBJECT_INDEX_SCALE))",
                fieldModel.type, valueBuilder.unsafe(), field, type, type);
    }

    /**
     * Adds an equality check using {@link java.util.Objects#equals(Object, Object)}.
     */
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

    /**
     * Returns the expression computing {@code Objects.hashCode} of this field.
     */
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
