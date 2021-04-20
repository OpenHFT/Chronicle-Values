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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import java.lang.reflect.Method;
import java.util.Objects;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;

class ValueFieldModel extends ScalarFieldModel {
    private final NativeMemberGenerator nativeGenerator = new NativeMemberGenerator();
    private ValueModel valueModel;

    private ValueModel valueModel() {
        if (valueModel == null)
            valueModel = ValueModel.acquire(type);
        return valueModel;
    }

    @Override
    int sizeInBits() {
        return valueModel().sizeInBytes() * 8;
    }

    @Override
    int offsetAlignmentInBytes() {
        if (offsetAlignment == Align.DEFAULT)
            return valueModel().recommendedOffsetAlignment();
        // Value fields should be aligned at least to a byte boundary
        return Math.max(1, offsetAlignment);
    }

    @Override
    NativeMemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new ObjectHeapMemberGenerator(this) {
            FieldSpec usingValue;

            @Override
            void generateFields(ValueBuilder valueBuilder) {
                field = FieldSpec.builder(valueModel().heapClass(), fieldName(), PRIVATE)
                        .initializer("new $T()", valueModel().heapClass())
                        .build();
                valueBuilder.typeBuilder.addField(field);
            }

            @Override
            void generateArrayElementFields(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
                field = FieldSpec.builder(ArrayTypeName.of(valueModel().heapClass()), fieldName())
                        .addModifiers(PRIVATE, FINAL)
                        .initializer("new $T[$L]",
                                valueModel().heapClass(), arrayFieldModel.array.length())
                        .build();
                valueBuilder.typeBuilder.addField(field);
                usingValue = FieldSpec.builder(valueModel().heapClass(), fieldName() + "Value", PRIVATE)
                        .initializer("new $T()", valueModel().heapClass())
                        .build();
                valueBuilder.typeBuilder.addField(usingValue);
                MethodSpec.Builder constructor = valueBuilder.defaultConstructorBuilder();
                constructor.beginControlFlow("for (int index = 0; index < $L; index++)",
                        arrayFieldModel.array.length());
                constructor.addStatement("$N[index] = new $T()",
                        fieldName(), valueModel().heapClass());
                constructor.endControlFlow();
            }

            @Override
            void generateArrayElementGetUsing(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("(($T) $N).copyFrom($N[index])",
                        Copyable.class, usingName(), field);
                if (arrayFieldModel.getUsing.getReturnType() != void.class) {
                    methodBuilder.addStatement("return $N", usingName());
                }
            }

            @Override
            public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N.copyFrom($N)", field, varName());
            }

            @Override
            public void generateArrayElementSet(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N[index].copyFrom($N)", field, varName());
            }

            @Override
            public void generateSetVolatile(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N.copyFrom($N)", field, varName());
                methodBuilder.addStatement("$N.storeFence()", valueBuilder.unsafe());
            }

            @Override
            public void generateArrayElementSetVolatile(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N[index].copyFrom($N)", field, varName());
                methodBuilder.addStatement("$N.storeFence()", valueBuilder.unsafe());
            }

            @Override
            public void generateSetOrdered(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                generateSetVolatile(valueBuilder, methodBuilder);
            }

            @Override
            public void generateArrayElementSetOrdered(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                generateArrayElementSetVolatile(arrayFieldModel, valueBuilder, methodBuilder);
            }

            @Override
            public void generateCompareAndSwap(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                throw new UnsupportedOperationException(
                        "compareAndSwap() is not supported by value field " + name);
            }

            @Override
            public void generateArrayElementCompareAndSwap(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                throw new UnsupportedOperationException(
                        "compareAndSwap() is not supported by value field " + name);
            }

            @Override
            public void generateCopyFrom(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N.copyFrom(from.$N())",
                        field, fieldModel.getOrGetVolatile().getName());
            }

            @Override
            void generateArrayElementEquals(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                if (arrayFieldModel.getUsing != null) {
                    methodBuilder.addStatement("other.$N(index, this.$N)",
                            arrayFieldModel.getUsing.getName(), usingValue);
                    methodBuilder.addCode("if (!$T.equals($N[index], this.$N)) return false;\n",
                            Objects.class, field, usingValue);
                } else {
                    methodBuilder.addCode("if (!$T.equals($N[index], other.$N(index))) return false;\n",
                            Objects.class, field, arrayFieldModel.getOrGetVolatile().getName());
                }
            }

            @Override
            public void generateArrayElementCopyFrom(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                if (arrayFieldModel.getUsing != null) {
                    methodBuilder.addStatement("from.$N(index, this.$N[index])",
                            arrayFieldModel.getUsing.getName(), field);
                } else {
                    methodBuilder.addStatement("this.$N[index].copyFrom(from.$N(index))",
                            field, arrayFieldModel.getOrGetVolatile().getName());
                }
            }

            @Override
            void generateWriteMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N.writeMarshallable(bytes)", fieldName());
            }

            @Override
            void generateArrayElementWriteMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N[index].writeMarshallable(bytes)", fieldName());
            }

            @Override
            void generateReadMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N.readMarshallable(bytes)", fieldName());
            }

            @Override
            void generateArrayElementReadMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N[index].readMarshallable(bytes)", fieldName());
            }

            @Override
            String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                return field.name + ".hashCode()";
            }

            @Override
            String generateArrayElementHashCode(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                return field.name + "[index].hashCode()";
            }
        };
    }

    final class NativeMemberGenerator extends MemberGenerator {

        FieldSpec cachedValue;
        FieldSpec otherCachedValue;
        private Class nativeType;

        NativeMemberGenerator() {
            super(ValueFieldModel.this);
        }

        @Override
        public void generateFields(ValueBuilder valueBuilder) {
            nativeType = Values.nativeClassFor(type);
            cachedValue = FieldSpec
                    .builder(nativeType, name + "CachedValue", PRIVATE, FINAL)
                    .initializer("new $T()", nativeType)
                    .build();
            valueBuilder.typeBuilder.addField(cachedValue);
            otherCachedValue = FieldSpec
                    .builder(nativeType, name + "OtherCachedValue", PRIVATE, FINAL)
                    .initializer("new $T()", nativeType)
                    .build();
            valueBuilder.typeBuilder.addField(otherCachedValue);
        }

        @Override
        public void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            generateFields(valueBuilder);
        }

        private void initCachedValue(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$T $N = this.$N", nativeType, cachedValue, cachedValue);
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.addStatement("$N.bytesStore(bs, offset + $L, $L)",
                    cachedValue, byteOffset, valueModel().sizeInBytes());
        }

        @Override
        public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedValue(valueBuilder, methodBuilder);
            methodBuilder.addStatement("return $N", cachedValue);
        }

        @Override
        public void generateArrayElementGet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            methodBuilder.addStatement("return $N", cachedValue);
        }

        private void initArrayElementCachedValue(
                ArrayFieldModel arrayField, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$T $N = this.$N", nativeType, cachedValue, cachedValue);
            int arrayByteOffset = arrayField.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayField, methodBuilder);
            methodBuilder.addStatement("$N.bytesStore(bs, offset + $L + elementOffset, $L)",
                    cachedValue, arrayByteOffset, valueModel().sizeInBytes());
        }

        @Override
        public void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            //TODO if both are Byteable, should do a shallow ("pointer") copy or memcpy?
            methodBuilder.beginControlFlow("if ($N instanceof $T)", varName(), nativeType);
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.addStatement("(($T) $N).bytesStore(bs, offset + $L, $L)",
                    nativeType, varName(), byteOffset, valueModel().sizeInBytes());
            methodBuilder.nextControlFlow("else");
            initCachedValue(valueBuilder, methodBuilder);
            methodBuilder.addStatement("(($T) $N).copyFrom($N)",
                    Copyable.class, varName(), cachedValue);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("return $N", varName());
        }

        @Override
        public void generateArrayElementGetUsing(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.beginControlFlow("if ($N instanceof $T)", usingName(), nativeType);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            methodBuilder.addStatement("(($T) $N).bytesStore(bs, offset + $L + elementOffset, $L)",
                    nativeType, usingName(), arrayByteOffset, valueModel().sizeInBytes());
            methodBuilder.nextControlFlow("else");
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            methodBuilder.addStatement(
                    "(($T) $N).copyFrom($N)", Copyable.class, usingName(), cachedValue);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("return $N", usingName());
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.beginControlFlow("if ($N instanceof $T)", varName(), nativeType);
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.addStatement(
                    "bs.write(offset + $L, (($T) $N).bytesStore(), (($T) $N).offset(), $L)",
                    byteOffset, nativeType, varName(), nativeType, varName(),
                    valueModel().sizeInBytes());
            methodBuilder.nextControlFlow("else");
            initCachedValue(valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N.copyFrom($N)", cachedValue, varName());
            methodBuilder.endControlFlow();
        }

        @Override
        public void generateArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.beginControlFlow("if ($N instanceof $T)", varName(), nativeType);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            methodBuilder.addStatement(
                    "bs.write(offset + $L + elementOffset, " +
                            "(($T) $N).bytesStore(), (($T) $N).offset(), $L)",
                    arrayByteOffset, nativeType, varName(), nativeType, varName(),
                    valueModel().sizeInBytes());
            methodBuilder.nextControlFlow("else");
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N.copyFrom($N)", cachedValue, varName());
            methodBuilder.endControlFlow();
        }

        @Override
        public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedValue(valueBuilder, methodBuilder);
            if (getUsing != null) {
                methodBuilder.addStatement("from.$N($N)", getUsing.getName(), cachedValue);
            } else {
                methodBuilder.addStatement("$N.copyFrom(from.$N())", cachedValue, get.getName());
            }
        }

        @Override
        public void generateArrayElementCopyFrom(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            Method getUsing = arrayFieldModel.getUsing;
            if (getUsing != null) {
                methodBuilder.addStatement("from.$N(index, $N)", getUsing.getName(), cachedValue);
            } else {
                methodBuilder.addStatement("$N.copyFrom(from.$N(index))",
                        cachedValue, arrayFieldModel.get.getName());
            }
        }

        @Override
        void generateWriteMarshallable(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedValue(valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N.writeMarshallable(bytes)", cachedValue);
        }

        @Override
        void generateArrayElementWriteMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N.writeMarshallable(bytes)", cachedValue);
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedValue(valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N.readMarshallable(bytes)", cachedValue);
        }

        @Override
        void generateArrayElementReadMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N.readMarshallable(bytes)", cachedValue);
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedValue(valueBuilder, methodBuilder);
            methodBuilder.addCode("if (!$N.equals(other.$N())) return false;\n",
                    cachedValue, get.getName());
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            Method getUsing = arrayFieldModel.getUsing;
            if (getUsing != null) {
                methodBuilder.addStatement("$T $N = this.$N",
                        type, otherCachedValue, otherCachedValue);
                methodBuilder.addStatement("other.$N(index, $N)",
                        getUsing.getName(), otherCachedValue);
                methodBuilder.addCode("if (!$N.equals($N)) return false;\n",
                        cachedValue, otherCachedValue);
            } else {
                methodBuilder.addCode("if (!$N.equals(other.$N(index))) return false;\n",
                        cachedValue, arrayFieldModel.get.getName());
            }
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedValue(valueBuilder, methodBuilder);
            return cachedValue.name + ".hashCode()";
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            return cachedValue.name + ".hashCode()";
        }

        @Override
        void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedValue(valueBuilder, methodBuilder);
            genToString(methodBuilder, cachedValue.name);
        }

        @Override
        void generateArrayElementToString(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedValue(arrayFieldModel, valueBuilder, methodBuilder);
            genArrayElementToString(methodBuilder, cachedValue.name);
        }
    }
}
