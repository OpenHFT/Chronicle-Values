/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Method;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.*;
import static net.openhft.chronicle.values.IntegerFieldModel.NORMAL_ACCESS_TYPE;
import static net.openhft.chronicle.values.Nullability.NULLABLE;

/**
 * Models an enum reference backed by an {@code int} ordinal.
 *
 * <p>A static "universe" array caches the constants for each enum type. The
 * array is initialised via {@link Enums#getUniverse(Class)} and stored as a
 * {@code private static final} field so that ordinal to enum lookups never
 * perform reflection.
 *
 * <p>When a field is nullable the sentinel ordinal {@code -1} encodes
 * {@code null}. Non-nullable fields store the ordinal directly.
 */
class EnumFieldModel extends IntegerBackedFieldModel {

    /** Metadata describing whether the field may be {@code null}. */
    final FieldNullability nullability = new FieldNullability(this);

    /**
     * Generates the native (off-heap) implementation. It also declares
     * the universe array used for ordinal to enum conversion.
     */
    final MemberGenerator nativeGenerator = new IntegerBackedNativeMemberGenerator(this, backend) {

        @Override
        public void generateFields(ValueBuilder valueBuilder) {
            addUniverseField(valueBuilder);
        }

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            addUniverseField(valueBuilder);
        }

        @Override
        void finishGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String value) {
            methodBuilder.addStatement("return $N", fromOrdinalOrMinusOne(methodBuilder, value));
        }

        @Override
        String startSet(MethodSpec.Builder methodBuilder) {
            return toOrdinalOrMinusOne(varName());
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
            methodBuilder.addCode("if (($N) != other.$N()) return false;\n",
                    value, getOrGetVolatile().getName());
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genArrayElementGet(
                            arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE));
            methodBuilder.addCode("if (($N) != other.$N(index)) return false;\n",
                    value, arrayFieldModel.getOrGetVolatile().getName());
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
            return format("java.util.Objects.hashCode(%s)", value);
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genArrayElementGet(
                            arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE));
            return format("java.util.Objects.hashCode(%s)", value);
        }
    };

    @Override
    void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        nullability.addInfo(m, template);
    }

    /**
     * Finalises the model once all type information is gathered.
     * <p>
     * The backing field is converted to an {@code int} range covering the
     * enum constants. If the field is nullable the range includes {@code -1}
     * for the {@code null} value.
     */
    @Override
    void postProcess() {
        super.postProcess();
        int min = nullable() ? -1 : 0;
        @SuppressWarnings({"rawtypes", "unchecked"})
        int constants = Enums.numberOfConstants((Class) type);
        if (constants == 0) {
            throw new IllegalStateException(
                    name + "field type is a enum with zero constants: " + type);
        }
        backend.type = int.class;
        backend.range = new RangeImpl(min, constants - 1);
        backend.postProcess();
    }

    private boolean nullable() {
        return nullability.nullability() == NULLABLE;
    }

    private String universeName() {
        return name + "Universe";
    }

    /**
     * Adds a static array holding all enum constants to the generated class.
     * <p>
     * The array is initialised once using {@link Enums#getUniverse(Class)} and
     * cached for every instance. It allows ordinal to enum conversion without
     * repeated reflective calls.
     */
    private void addUniverseField(ValueBuilder valueBuilder) {
        FieldSpec universe = FieldSpec
                .builder(ArrayTypeName.of(type), universeName())
                .addModifiers(PRIVATE, STATIC, FINAL)
                .initializer("$T.getUniverse($T.class)", Enums.class, type)
                .build();
        valueBuilder.typeBuilder.addField(universe);
    }

    /**
     * Converts an enum reference to the stored ordinal value.
     *
     * <p>Nullable fields use {@code -1} as the sentinel for {@code null};
     * otherwise the enum's ordinal is returned unchanged.
     *
     * @param e expression yielding an enum instance
     * @return ordinal or {@code -1} when {@code null} is permitted and the
     * instance is {@code null}
     */
    private String toOrdinalOrMinusOne(String e) {
        if (nullable()) {
            return format("(%s != null ? %s.ordinal() : -1)", e, e);
        } else {
            return e + ".ordinal()";
        }
    }

    /**
     * Converts an ordinal previously stored in the backing field back to an
     * enum constant or {@code null}.
     *
     * <p>The sentinel {@code -1} maps back to {@code null}; any other value is
     * used as an index into the cached universe array.
     *
     * @param methodBuilder context used to declare temporary variables
     * @param value         expression yielding the ordinal
     * @return Java expression that evaluates to the enum value
     */
    private String fromOrdinalOrMinusOne(MethodSpec.Builder methodBuilder, String value) {
        if (nullable()) {
            String ordinalVariableName = name() + "Ordinal";
            methodBuilder.addStatement(format("int %s = %s", ordinalVariableName, value));
            return format("%s >= 0 ? %s[%s] : null", ordinalVariableName, universeName(), ordinalVariableName);
        } else {
            return format("%s[%s]", universeName(), value);
        }
    }

    /**
     * Returns the generator responsible for the native implementation.
     */
    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    /**
     * Builds the generator for heap-based implementations which mirrors the
     * native behaviour while using on-heap storage.
     */
    @Override
    MemberGenerator createHeapGenerator() {
        return new ObjectHeapMemberGenerator(this) {

            @Override
            public void generateFields(ValueBuilder valueBuilder) {
                super.generateFields(valueBuilder);
                addUniverseField(valueBuilder);
            }

            @Override
            void generateArrayElementFields(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
                super.generateArrayElementFields(arrayFieldModel, valueBuilder);
                addUniverseField(valueBuilder);
            }

            @Override
            void generateWriteMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("bytes.writeStopBit($N)",
                        toOrdinalOrMinusOne(fieldName()));
            }

            @Override
            void generateArrayElementWriteMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("bytes.writeStopBit($N))",
                        toOrdinalOrMinusOne(fieldName() + "[index]"));
            }

            @Override
            void generateReadMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N = $N", fieldName(),
                        fromOrdinalOrMinusOne(methodBuilder, "(int) bytes.readStopBit()"));
            }

            @Override
            void generateArrayElementReadMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N[index] = $N", fieldName(),
                        fromOrdinalOrMinusOne(methodBuilder, "(int) bytes.readStopBit()"));
            }
        };
    }
}
