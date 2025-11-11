/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import com.squareup.javapoet.MethodSpec;

/**
 * Base class used by the code generator to create field access methods.
 * <p>
 * Each instance operates on a single {@link FieldModel} and emits the
 * JavaPoet statements required to implement the declared accessors. Methods
 * that are not overridden throw {@link UnsupportedOperationException}.
 */
abstract class MemberGenerator {

    /** Description of the field for which accessors are generated. */
    final FieldModel fieldModel;

    protected MemberGenerator(FieldModel fieldModel) {
        this.fieldModel = fieldModel;
    }

    private Class<? extends FieldModel> fieldModelClass() {
        return fieldModel.getClass();
    }

    /**
     * Adds private fields to the generated class if required.
     *
     * @param valueBuilder builder that collects field declarations
     */
    void generateFields(ValueBuilder valueBuilder) {
        // default implementation adds no fields
    }

    /**
     * Adds fields when this model represents an array element.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    builder that collects field declarations
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementFields(ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the accessor body for {@code get}.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the accessor body for array element {@code get}.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the accessor body for {@code getVolatile}.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the accessor body for volatile array element reads.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for {@code getUsing}.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for array element {@code getUsing}.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementGetUsing(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for the standard {@code set} method.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for the array element {@code set} method.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for {@code setVolatile}.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for volatile array element writes.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for ordered writes.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the ordered write body for an array element.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for an additive update.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for additive updates on array elements.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementAdd(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for an atomic additive update.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for atomic additive updates on array elements.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementAddAtomic(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits the body for a compare-and-swap update.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateCompareAndSwap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits compare-and-swap logic for an array element.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementCompareAndSwap(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Copies a field from another value instance.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Copies an array element from another value instance.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Writes the field to a {@code Bytes} instance.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     */
    void generateWriteMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("bytes.$N($N())",
                fieldModel.writeMethod(), fieldModel.getOrGetVolatile().getName());
    }

    /**
     * Writes an array element to a {@code Bytes} instance.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     */
    void generateArrayElementWriteMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("bytes.$N($N(index))",
                fieldModel.writeMethod(), arrayFieldModel.getOrGetVolatile().getName());
    }

    /**
     * Reads the field from a {@code Bytes} instance.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Reads an array element from a {@code Bytes} instance.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementReadMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits code for field comparison.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits comparison logic for an array element.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @throws UnsupportedOperationException unless overridden
     */
    void generateArrayElementEquals(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits code calculating a hash for the field.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     * @return expression used as the hash result
     * @throws UnsupportedOperationException unless overridden
     */
    String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Emits hash code logic for an array element.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     * @return expression used as the hash result
     * @throws UnsupportedOperationException unless overridden
     */
    String generateArrayElementHashCode(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    /**
     * Appends a textual representation of the field to a {@code StringBuilder}.
     *
     * @param valueBuilder  context of the generated value
     * @param methodBuilder destination for statements
     */
    void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        genToString(methodBuilder, fieldModel.getOrGetVolatile().getName() + "()");
    }

    final void genToString(MethodSpec.Builder methodBuilder, String value) {
        methodBuilder.addStatement("sb.append($S).append($N)",
                ", " + fieldModel.name + "=", value);
    }

    /**
     * Appends an array element to a {@code StringBuilder}.
     *
     * @param arrayFieldModel owning array description
     * @param valueBuilder    context of the generated value
     * @param methodBuilder   destination for statements
     */
    void generateArrayElementToString(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        genArrayElementToString(methodBuilder,
                arrayFieldModel.getOrGetVolatile().getName() + "(index)");
    }

    final void genArrayElementToString(MethodSpec.Builder methodBuilder, String value) {
        methodBuilder.addStatement("sb.append($N).append(',').append(' ')", value);
    }
}
