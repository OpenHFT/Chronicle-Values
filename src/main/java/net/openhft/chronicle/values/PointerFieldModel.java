/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.bytes.Byteable;

import static java.lang.String.format;
import static net.openhft.chronicle.values.IntegerFieldModel.NORMAL_ACCESS_TYPE;
import static net.openhft.chronicle.values.Utils.capitalize;

/**
 * Implementation detail for {@code @Pointer} fields.
 * <p>
 * The value stored in the generated class is a {@code long} containing a memory
 * address. During writes the setter verifies that the provided object
 * implements {@link Byteable} and extracts its {@link Byteable#address()}. A
 * zero address represents a {@code null} reference.
 * <p>
 * Marshalling writes a presence flag followed by the pointed value when the
 * flag is {@code true}. If the flag indicates a value yet the stored address is
 * zero an {@link IllegalStateException} is thrown, ensuring the pointer offset
 * has been initialised correctly.
 * <p>
 * Copy operations such as {@code copyFrom} merely transfer the stored address;
 * the bytes referenced by the pointer are not cloned.
 */
final class PointerFieldModel extends IntegerBackedFieldModel {

    /** Model of the referenced value interface. */
    private final ValueFieldModel pointedModel;
    final MemberGenerator nativeGenerator = new IntegerBackedNativeMemberGenerator(this, backend) {

        @Override
        void generateFields(ValueBuilder valueBuilder) {
            super.generateFields(valueBuilder);
            pointedModel.nativeGenerator().generateFields(valueBuilder);
        }

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            generateFields(valueBuilder);
        }

        /**
         * Returns the pointed value from {@code address}. When the address is zero
         * {@code null} is returned.
         */
        @Override
        void finishGet(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String address) {
            String addressVariable = name + "Address";
            methodBuilder.addStatement("long $N = $N", addressVariable, address);
            methodBuilder.beginControlFlow("if ($N != 0)", addressVariable);
            {
                initCachedValue(valueBuilder, methodBuilder, address);
                methodBuilder.addStatement("return $N", cachedValue());
            }
            methodBuilder.nextControlFlow("else");
            {
                methodBuilder.addStatement("return null");
            }
            methodBuilder.endControlFlow();
        }

        /**
         * Extracts the memory address from the parameter supplied to the setter.
         * A {@code null} value results in a zero address.
         */
        @Override
        String startSet(MethodSpec.Builder methodBuilder) {
            return extractAddress(methodBuilder, varName());
        }

        @Override
        void generateWriteMarshallable(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String address = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            genWriteMarshallable(valueBuilder, methodBuilder, address, cachedValue());
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String address = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            genReadMarshallable(valueBuilder, methodBuilder, address, cachedValue(), () ->
                    backingFieldModel.genSet(valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE, "0L")
            );
        }

        @Override
        void generateArrayElementWriteMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String address = backingFieldModel.genArrayElementGet(
                    arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
            genWriteMarshallable(valueBuilder, methodBuilder, address, cachedValue());
        }

        @Override
        void generateArrayElementReadMarshallable(
                ArrayFieldModel arrayFieldModel,
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String address = backingFieldModel.genArrayElementGet(
                    arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
            genReadMarshallable(valueBuilder, methodBuilder, address, cachedValue(),
                    () -> backingFieldModel.genArrayElementSet(
                            arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE, "0L")
            );
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String otherValueVariable = "other" + capitalize(name);
            methodBuilder.addStatement("$T $N = other.$N()",
                    type, otherValueVariable, getOrGetVolatile().getName());
            String otherAddress = extractAddress(methodBuilder, otherValueVariable);
            String thisAddress = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            methodBuilder.addCode("if ($N != $N) return false;\n", thisAddress, otherAddress);
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String otherValueVariable = "other" + capitalize(name);
            methodBuilder.addStatement("$T $N = other.$N(index)",
                    type, otherValueVariable, getOrGetVolatile().getName());
            String otherAddress = extractAddress(methodBuilder, otherValueVariable);
            String thisAddress = backingFieldModel.genArrayElementGet(
                    arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
            methodBuilder.addCode("if ($N != $N) return false;\n", thisAddress, otherAddress);
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String address = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            return format("Long.hashCode(%s)", address);
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String address = backingFieldModel.genArrayElementGet(
                    arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
            return format("Long.hashCode(%s)", address);
        }
    };

    PointerFieldModel(ValueFieldModel pointedModel) {
        this.pointedModel = pointedModel;
    }

    @Override
    void postProcess() {
        super.postProcess();
        pointedModel.postProcess();
        backend.type = long.class;
        backend.range = RangeImpl.DEFAULT_LONG_RANGE;
        backend.postProcess();
    }

    @Override
    void checkState() {
        super.checkState();
        pointedModel.checkState();
    }

    private FieldSpec cachedValue() {
        return pointedModel.nativeGenerator().cachedValue;
    }

    /**
     * Prepares the cached value to operate on the memory at {@code address}.
     * Copies the pointed bytes into the private buffer for use by generated code.
     */
    private void initCachedValue(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String address) {
        methodBuilder.addStatement("$N.set($N, $L)", valueBuilder.bytesStoreForPointers(), address, pointedModel.sizeInBytes());
        methodBuilder.addStatement("$N.bytesStore($N, 0, $L)", cachedValue(),
                valueBuilder.bytesStoreForPointers(), pointedModel.sizeInBytes());
    }

    /**
     * Validates {@code value} as {@link Byteable} and returns its address.
     * When {@code value} is {@code null} the returned variable contains zero.
     */
    private String extractAddress(MethodSpec.Builder methodBuilder, String value) {
        String addressVariable = value + "Address";
        methodBuilder.addStatement("long $N", addressVariable);
        methodBuilder.beginControlFlow("if ($N != null)", value);
        {
            methodBuilder.beginControlFlow("if (!($N instanceof $T))", value, Byteable.class);
            String message =
                    "\"$N should be instance of $T, \" + $N.getClass() + \" is given\"";
            methodBuilder.addStatement("throw new $T(" + message + ")",
                    IllegalArgumentException.class, name, Byteable.class, value);
            methodBuilder.endControlFlow();

            methodBuilder.addStatement(
                    "$N = (($T) $N).address()",
                    addressVariable, Byteable.class, value);
        }
        methodBuilder.nextControlFlow("else");
        {
            methodBuilder.addStatement("$N = 0L", addressVariable);
        }
        methodBuilder.endControlFlow();
        return addressVariable;
    }

    /**
     * Writes the boolean presence flag and, when non-zero, serialises the pointed
     * value using the cached buffer.
     */
    private void genWriteMarshallable(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
            String address, Object value) {
        String addressVariable = name + "Address";
        methodBuilder.addStatement("long $N = $N", addressVariable, address);
        methodBuilder.beginControlFlow("if ($N != 0)", addressVariable);
        {
            initCachedValue(valueBuilder, methodBuilder, address);
            methodBuilder.addStatement("bytes.writeBoolean(true)");
            methodBuilder.addStatement("$N.writeMarshallable(bytes)", value);
        }
        methodBuilder.nextControlFlow("else");
        {
            methodBuilder.addStatement("bytes.writeBoolean(false)");
        }
        methodBuilder.endControlFlow();
    }

    /**
     * Reads the boolean presence flag and populates the cached value from
     * {@code address}. If the flag is false the provided {@code setNull}
     * action is executed.
     */
    private void genReadMarshallable(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
            String address, Object value, Runnable setNull) {
        String present = name + "Present";
        methodBuilder.addStatement("boolean $N = bytes.readBoolean()", present);
        methodBuilder.beginControlFlow("if ($N)", present);
        {
            String addressVariable = name + "Address";
            methodBuilder.addStatement("long $N = $N", addressVariable, address);
            methodBuilder.beginControlFlow("if ($N != 0)", addressVariable);
            {
                initCachedValue(valueBuilder, methodBuilder, address);
                methodBuilder.addStatement("$N.readMarshallable(bytes)", value);
            }
            methodBuilder.nextControlFlow("else");
            {
                methodBuilder.addStatement("throw new $T($S)", IllegalStateException.class,
                        name + " field should be initialized to some pointer when reading " +
                                "non-null value from marshalled bytes");
            }
            methodBuilder.endControlFlow();
        }
        methodBuilder.nextControlFlow("else");
        {
            setNull.run();
        }
        methodBuilder.endControlFlow();
    }

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new PrimitiveBackedHeapMemberGenerator(this, backend.type) {

            @Override
            void generateFields(ValueBuilder valueBuilder) {
                super.generateFields(valueBuilder);
                pointedModel.nativeGenerator().generateFields(valueBuilder);
            }

            @Override
            void generateArrayElementFields(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
                super.generateArrayElementFields(arrayFieldModel, valueBuilder);
                pointedModel.nativeGenerator()
                        .generateArrayElementFields(arrayFieldModel, valueBuilder);
            }

            @Override
            String wrap(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
                        String rawStoredValue) {
                String result = name + "Result";
                methodBuilder.addStatement("$T $N", type, result);
                methodBuilder.beginControlFlow("if ($N != 0)", rawStoredValue);
                {
                    initCachedValue(valueBuilder, methodBuilder, rawStoredValue);
                    methodBuilder.addStatement("$N = $N", result, cachedValue());
                }
                methodBuilder.nextControlFlow("else");
                {
                    methodBuilder.addStatement("$N = null", result);
                }
                methodBuilder.endControlFlow();
                return result;
            }

            @Override
            String unwrap(MethodSpec.Builder methodBuilder, String inputValue) {
                return extractAddress(methodBuilder, inputValue);
            }

            private void genWriteMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String address) {
                String addressVariable = name + "Address";
                methodBuilder.addStatement("long $N = $N", addressVariable, address);
                methodBuilder.beginControlFlow("if ($N != 0)", addressVariable);
                {
                    initCachedValue(valueBuilder, methodBuilder, addressVariable);
                    methodBuilder.addStatement("bytes.writeBoolean(true)");
                    methodBuilder.addStatement("$N.writeMarshallable(bytes)", cachedValue());
                }
                methodBuilder.nextControlFlow("else");
                {
                    methodBuilder.addStatement("bytes.writeBoolean(false)");
                }
                methodBuilder.endControlFlow();
            }

            @Override
            void generateWriteMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                genWriteMarshallable(valueBuilder, methodBuilder, field.name);
            }

            @Override
            void generateArrayElementWriteMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                genWriteMarshallable(valueBuilder, methodBuilder, field.name + "[index]");
            }

            private void genReadMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
                    String address, Runnable setNull) {
                String present = name + "Present";
                methodBuilder.addStatement("boolean $N = bytes.readBoolean()", present);
                methodBuilder.beginControlFlow("if ($N)", present);
                {
                    String addressVariable = name + "Address";
                    methodBuilder.addStatement("long $N = $N", addressVariable, address);
                    methodBuilder.beginControlFlow("if ($N != 0)", addressVariable);
                    {
                        initCachedValue(valueBuilder, methodBuilder, addressVariable);
                        methodBuilder.addStatement("$N.readMarshallable(bytes)", cachedValue());
                    }
                    methodBuilder.nextControlFlow("else");
                    {
                        methodBuilder.addStatement("throw new $T($S)", IllegalStateException.class,
                                name + " field should be initialized to some pointer " +
                                        "when reading non-null value from marshalled bytes");
                    }
                    methodBuilder.endControlFlow();
                }
                methodBuilder.nextControlFlow("else");
                {
                    setNull.run();
                }
                methodBuilder.endControlFlow();
            }

            @Override
            void generateReadMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                genReadMarshallable(valueBuilder, methodBuilder, field.name,
                        () -> methodBuilder.addStatement("$N = 0L", field));
            }

            @Override
            void generateArrayElementReadMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                genReadMarshallable(valueBuilder, methodBuilder, field.name + "[index]",
                        () -> methodBuilder.addStatement("$N[index] = 0L", field));
            }

            @Override
            String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                return format("Long.hashCode(%s)", field.name);
            }

            @Override
            String generateArrayElementHashCode(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                return format("Long.hashCode(%s[index])", field.name);
            }
        };
    }
}
