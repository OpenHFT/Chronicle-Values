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

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.bytes.Byteable;

import static java.lang.String.format;
import static net.openhft.chronicle.values.IntegerFieldModel.NORMAL_ACCESS_TYPE;
import static net.openhft.chronicle.values.Utils.capitalize;

final class PointerFieldModel extends IntegerBackedFieldModel {

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

    private void initCachedValue(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String address) {
        methodBuilder.addStatement("$N.bytesStore($N, $N, $L)", cachedValue(),
                valueBuilder.bytesStoreForPointers(), address, pointedModel.sizeInBytes());
    }

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
                    "$N = (($T) $N).bytesStore().addressForRead((($T) $N).offset())",
                    addressVariable, Byteable.class, value, Byteable.class, value);
        }
        methodBuilder.nextControlFlow("else");
        {
            methodBuilder.addStatement("$N = 0L", addressVariable);
        }
        methodBuilder.endControlFlow();
        return addressVariable;
    }

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
