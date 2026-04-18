/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import com.squareup.javapoet.MethodSpec;

import static java.lang.String.format;
import static net.openhft.chronicle.values.IntegerFieldModel.*;

/**
 * Generates native accessors backed by an integer field. The actual
 * field type is converted to and from this integer when reads and
 * writes occur. Subclasses provide the conversion logic.
 */
abstract class IntegerBackedNativeMemberGenerator extends MemberGenerator {

    /**
     * Integer field used to hold the raw value in native memory.
     */
    final IntegerFieldModel backingFieldModel;

    IntegerBackedNativeMemberGenerator(
            FieldModel fieldModel, IntegerFieldModel backingFieldModel) {
        super(fieldModel);
        this.backingFieldModel = backingFieldModel;
    }

    /**
     * Adds code to convert the supplied integer expression to the accessor
     * return type.
     *
     * @param value expression evaluating to the raw integer
     */
    abstract void finishGet(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String value);

    /**
     * Converts the argument to its integer representation and returns the
     * string to be written.
     */
    abstract String startSet(MethodSpec.Builder methodBuilder);

    @Override
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateGetVolatile(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genGet(valueBuilder, VOLATILE_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, VOLATILE_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                NORMAL_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateSetVolatile(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, VOLATILE_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                VOLATILE_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateSetOrdered(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, ORDERED_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                ORDERED_ACCESS_TYPE, valueToWrite);
    }

    @Override
    void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        if (fieldModel.set != null) {
            methodBuilder.addStatement("$N(from.$N())",
                    fieldModel.set.getName(), fieldModel.getOrGetVolatile().getName());
        } else {
            methodBuilder.addStatement("$T $N = from.$N()",
                    fieldModel.type, fieldModel.varName(), fieldModel.getOrGetVolatile().getName());
            generateSet(valueBuilder, methodBuilder);
        }
    }

    @Override
    void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        if (arrayFieldModel.set != null) {
            methodBuilder.addStatement("$N(index, from.$N(index))",
                    arrayFieldModel.set.getName(), arrayFieldModel.getOrGetVolatile().getName());
        } else {
            methodBuilder.addStatement("$T $N = from.$N(index)",
                    fieldModel.type, fieldModel.varName(),
                    arrayFieldModel.getOrGetVolatile().getName());
            generateArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder);
        }
    }

    @Override
    void generateWriteMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("bytes.$N($N)", backingFieldModel.writeMethod(),
                backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
    }

    @Override
    void generateArrayElementWriteMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
        methodBuilder.addStatement("bytes.$N($N)", backingFieldModel.writeMethod(), value);
    }

    @Override
    void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        backingFieldModel.genSet(valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE, readValue());
    }

    @Override
    void generateArrayElementReadMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                NORMAL_ACCESS_TYPE, readValue());
    }

    private String readValue() {
        return format("bytes.%s()", backingFieldModel.readMethod());
    }
}
