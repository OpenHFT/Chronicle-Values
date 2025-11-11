/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.core.Jvm;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.*;
import static net.openhft.chronicle.values.Utils.capitalize;

/**
 * Base for generators that emit heap-backed field accessors.
 * <p>
 * Subclasses provide conversions between the stored form and the value
 * interface type and supply the appropriate {@code Unsafe} method names.
 */
abstract class HeapMemberGenerator extends MemberGenerator {

    /** The field added to the generated class. */
    FieldSpec field;

    /**
     * Offset constant for {@link #field}. Created lazily and inserted once into
     * the generated type for use by unsafe operations.
     */
    private FieldSpec fieldAddress;

    HeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    /**
     * @return name of the Unsafe method used for a volatile write
     */
    abstract String putVolatile();

    /**
     * @return name of the Unsafe ordered write method
     */
    abstract String putOrdered();

    /**
     * @return name of the compare-and-swap Unsafe method
     */
    abstract String compareAndSwap();

    /**
     * @return constant containing the base offset for an array of the stored type
     */
    abstract String arrayBase();

    /**
     * @return constant containing the index scale for an array of the stored type
     */
    abstract String arrayScale();

    /**
     * Ensures the field offset constant is generated and returns it.
     *
     * @param valueBuilder builder for the enclosing heap type
     * @return field offset constant
     */
    FieldSpec fieldOffset(ValueBuilder valueBuilder) {
        if (fieldAddress == null) {
            fieldAddress = FieldSpec.builder(long.class, fieldModel.name + "Address")
                    .addModifiers(PRIVATE, STATIC, FINAL)
                    .build();
            valueBuilder.staticBlockBuilder().addStatement(
                    "$N = $N.objectFieldOffset($T.getField($N.class, $S))",
                    fieldAddress, valueBuilder.unsafe(), Jvm.class, valueBuilder.className,
                    field.name);
            valueBuilder.typeBuilder.addField(fieldAddress);
        }
        return fieldAddress;
    }

    Class<?> fieldType() {
        return fieldModel.type;
    }

    /**
     * Converts the raw stored value to the interface type.
     */
    abstract String wrap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String rawStoredValue);

    /**
     * Converts a user-supplied value to its stored representation.
     */
    abstract String unwrap(MethodSpec.Builder methodBuilder, String inputValue);

    @Override
    void generateFields(ValueBuilder valueBuilder) {
        field = FieldSpec.builder(fieldType(), fieldModel.fieldName(), PRIVATE).build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    void generateArrayElementFields(ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
        field = FieldSpec.builder(ArrayTypeName.of(fieldType()), fieldModel.fieldName())
                .addModifiers(PRIVATE, FINAL)
                .initializer("new $T[$L]", fieldType(), arrayFieldModel.array.length())
                .build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N", fieldType(), rawValue, field);
        methodBuilder.addStatement("return $N", wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N[index]", fieldType(), rawValue, field);
        methodBuilder.addStatement("return $N", wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N = $N",
                field, unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N[index] = $N",
                field, unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$N.$N(this, $N, $N)",
                valueBuilder.unsafe(), putVolatile(), fieldOffset(valueBuilder),
                unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        Class<?> type = Utils.UNSAFE_CLASS;
        methodBuilder.addStatement(
                format("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s)",
                        unwrap(methodBuilder, fieldModel.varName())),
                valueBuilder.unsafe(), putVolatile(), field, type, arrayBase(),
                type, arrayScale());
    }

    @Override
    public void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$N.$N(this, $N, $N)",
                valueBuilder.unsafe(), putOrdered(), fieldOffset(valueBuilder),
                unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        Class<?> type = Utils.UNSAFE_CLASS;
        methodBuilder.addStatement(
                format("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s)",
                        unwrap(methodBuilder, fieldModel.varName())),
                valueBuilder.unsafe(), putOrdered(), field, type, arrayBase(), type,
                arrayScale());
    }

    @Override
    public void generateCompareAndSwap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String unwrappedOld = unwrap(methodBuilder, fieldModel.oldName());
        String unwrappedNew = unwrap(methodBuilder, fieldModel.newName());
        methodBuilder.addStatement("return $N.$N(this, $N, $N, $N)",
                valueBuilder.unsafe(), compareAndSwap(), fieldOffset(valueBuilder),
                unwrappedOld, unwrappedNew);
    }

    @Override
    public void generateArrayElementCompareAndSwap(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String unwrappedOld = unwrap(methodBuilder, fieldModel.oldName());
        String unwrappedNew = unwrap(methodBuilder, fieldModel.newName());
        Class<?> type = Utils.UNSAFE_CLASS;
        methodBuilder.addStatement(
                "return $N.$N($N, (long) $T.$N + (index * (long) $T.$N), $N, $N)",
                valueBuilder.unsafe(), compareAndSwap(), field, type, arrayBase(),
                type, arrayScale(), unwrappedOld, unwrappedNew);
    }

    @Override
    public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String copy = fieldModel.name + "Copy";
        methodBuilder.addStatement("$T $N = from.$N()",
                fieldModel.type, copy, fieldModel.getOrGetVolatile().getName());
        methodBuilder.addStatement("this.$N = $N", field, unwrap(methodBuilder, copy));
    }

    @Override
    public void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String copy = arrayFieldModel.name + "Copy";
        methodBuilder.addStatement("$T $N = from.$N(index)",
                arrayFieldModel.type, copy, arrayFieldModel.getOrGetVolatile().getName());
        methodBuilder.addStatement("this.$N[index] = $N", field, unwrap(methodBuilder, copy));
    }

    @Override
    void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N", fieldType(), rawValue, field);
        genToString(methodBuilder, wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    void generateArrayElementToString(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N[index]", fieldType(), rawValue, field);
        genArrayElementToString(methodBuilder, wrap(valueBuilder, methodBuilder, rawValue));
    }
}
