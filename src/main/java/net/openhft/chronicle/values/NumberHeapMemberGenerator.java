/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import com.squareup.javapoet.MethodSpec;

/**
 * Generates heap accessors for primitive numeric fields. In addition to the
 * standard getters and setters this class emits add methods in two forms:
 * <ul>
 * <li>a non-atomic variant that reads the current value, adds the supplied
 * argument and stores the result</li>
 * <li>an atomic variant backed by {@code Unsafe.getAndAddX}</li>
 * </ul>
 */
class NumberHeapMemberGenerator extends PrimitiveBackedHeapMemberGenerator {

    NumberHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    /**
     * @return name of the {@code Unsafe} method used for atomic addition to this
     * primitive type
     */
    private String getAndAdd() {
        return "getAndAdd" + capType;
    }

    /**
     * Emits a method that adds {@code addition} to the field and returns the new
     * value. Small integral types are widened for the calculation and cast back
     * before storing.
     */
    @Override
    public void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        if (fieldModel.type != byte.class && fieldModel.type != char.class &&
                fieldModel.type != short.class) {
            methodBuilder.addStatement(
                    "$T $N = " + wrap(valueBuilder, methodBuilder, "$N") + " + addition",
                    fieldModel.type, fieldModel.varName(), field);
        } else {
            methodBuilder.addStatement(
                    "$T $N = ($T) (" + wrap(valueBuilder, methodBuilder, "$N") + " + addition)",
                    fieldModel.type, fieldModel.varName(), fieldModel.type, field);
        }
        methodBuilder.addStatement(
                "$N = " + unwrap(methodBuilder, "$N"), field, fieldModel.varName());
        methodBuilder.addStatement("return $N", fieldModel.varName());
    }

    @Override
    public void generateArrayElementAdd(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(
                "$T $N = " + wrap(valueBuilder, methodBuilder, "$N[index]") + " + addition",
                fieldModel.type, fieldModel.varName(), field);
        methodBuilder.addStatement("$N[index] = " + unwrap(methodBuilder, "$N"),
                field, fieldModel.varName());
        methodBuilder.addStatement("return $N", fieldModel.varName());
    }

    /**
     * Generates an atomic add method backed by {@code Unsafe.getAndAddX}. The
     * method body returns the value after the addition has been applied.
     */
    @Override
    public void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return " +
                        wrap(valueBuilder, methodBuilder, "$N.$N(this, $N, addition) + addition"),
                valueBuilder.unsafe(), getAndAdd(), fieldOffset(valueBuilder));
    }

    @Override
    public void generateArrayElementAddAtomic(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        Class<?> type = Utils.UNSAFE_CLASS;
        methodBuilder.addStatement(
                "return " + wrap(valueBuilder, methodBuilder, "$N.$N($N, (long) $T.$N + " +
                        "(index * (long) $T.$N), addition) + addition"),
                valueBuilder.unsafe(), getAndAdd(), field, type, arrayBase(),
                type, arrayScale());
    }
}
