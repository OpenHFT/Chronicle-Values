/*
 *      Copyright (C) 2015, 2016  higherfrequencytrading.com
 *      Copyright (C) 2016 Roman Leventov
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.values;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.bytes.BytesUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.openhft.chronicle.values.Nullability.NULLABLE;

class CharSequenceFieldModel extends ScalarFieldModel {
    final FieldNullability nullability = new FieldNullability(this);
    MaxUtf8Length maxUtf8Length;

    @Override
    void addTypeInfo(Method m, MethodTemplate template) {
        if (!template.regex.startsWith("getUsing"))
            super.addTypeInfo(m, template);
        nullability.addInfo(m, template);
        Parameter annotatedParameter = template.annotatedParameter.apply(m);
        if (annotatedParameter == null)
            return;
        MaxUtf8Length paramMaxUtf8Length = annotatedParameter.getAnnotation(MaxUtf8Length.class);
        if (paramMaxUtf8Length != null) {
            if (maxUtf8Length != null) {
                throw new IllegalStateException(
                        "@MaxUtf8Length should be specified only once for " + name + " field. " +
                                "Specified " + maxUtf8Length + " and " + paramMaxUtf8Length);
            }
            if (paramMaxUtf8Length.value() <= 0)
                throw new IllegalStateException(
                        paramMaxUtf8Length + " max size should be positive");
            maxUtf8Length = paramMaxUtf8Length;
        }
    }

    @Override
    int sizeInBits() {
        if (maxUtf8Length == null)
            throw new IllegalStateException("@MaxUtf8Length must be specified for a field " + name);
        int sizeInBytes = BytesUtil.stopBitLength(maxUtf8Length.value()) + maxUtf8Length.value();
        return sizeInBytes * 8;
    }

    @Override
    int offsetAlignmentInBytes() {
        if (offsetAlignment == Align.DEFAULT) {
            throw new IllegalStateException("Default offset alignment doesn't make sense for " +
                    "CharSequence field " + name);
        }
        return Math.max(offsetAlignment, 1);
    }

    @Override
    void checkState() {
        super.checkState();
        checkUnsupported(getVolatile);
        checkUnsupported(setVolatile);
        checkUnsupported(setOrdered);
        checkUnsupported(add);
        checkUnsupported(addAtomic);
        checkUnsupported(compareAndSwap);
    }

    private void checkUnsupported(Method m) {
        if (m != null) {
            throw new IllegalStateException(type.getSimpleName() + "-typed field " +
                    name + "cannot have method " + m.getName());
        }
    }

    private String cachedStringBuilder() {
        return varName() + "Builder";
    }

    private String cachedBuilderToSettable() {
        if (type == String.class) {
            return cachedStringBuilder() + ".toString()";
        } else {
            return cachedStringBuilder();
        }
    }

    private void addCachedStringBuilder(ValueBuilder valueBuilder) {
        FieldSpec cachedStringBuilder = FieldSpec
                .builder(StringBuilder.class, cachedStringBuilder(), PRIVATE, FINAL)
                .initializer("new $T($L)", StringBuilder.class, maxUtf8Length.value())
                .build();
        valueBuilder.typeBuilder.addField(cachedStringBuilder);
    }

    private final MemberGenerator nativeGenerator = new MemberGenerator(this) {

        @Override
        public void generateFields(ValueBuilder valueBuilder) {
            addCachedStringBuilder(valueBuilder);
        }

        @Override
        public void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            generateFields(valueBuilder);
        }

        @Override
        public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedStringBuilder(valueBuilder, methodBuilder);
            finishGet(methodBuilder, get);
        }

        private void initCachedStringBuilder(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.beginControlFlow("if (bs.readUtf8Limited(offset + $L, $N, $L) > 0)",
                    byteOffset, cachedStringBuilder(), maxUtf8Length.value());
        }

        @Override
        public void generateArrayElementGet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            initArrayElementCachedStringBuilder(arrayFieldModel, valueBuilder, methodBuilder);
            finishGet(methodBuilder, arrayFieldModel.get);
        }

        private void initArrayElementCachedStringBuilder(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            methodBuilder.beginControlFlow(
                    "if (bs.readUtf8Limited(offset + $L + elementOffset, $N, $L) > 0)",
                    arrayByteOffset, cachedStringBuilder(), maxUtf8Length.value());
        }

        private void finishGet(MethodSpec.Builder methodBuilder, Method get) {
            if (type == String.class) {
                methodBuilder.addStatement("return $N.toString()", cachedStringBuilder());
            } else {
                if (type != StringBuilder.class && type != CharSequence.class) {
                    throw new IllegalStateException("Only StringBuilder, String and CharSequence " +
                            "classes are supported, " + name + " field type is " + type);
                }
                methodBuilder.addStatement("return $N", cachedStringBuilder());
            }
            nullGetBranch(methodBuilder, get);
        }

        @Override
        public void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.beginControlFlow("if (bs.readUtf8Limited(offset + $L, $N, $L) > 0)",
                    byteOffset, usingName(), maxUtf8Length.value());
            finishGetUsing(methodBuilder, getUsing);
        }

        @Override
        public void generateArrayElementGetUsing(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            methodBuilder.beginControlFlow(
                    "if (bs.readUtf8Limited(offset + $L + elementOffset, $N, $L) > 0)",
                    arrayByteOffset, usingName(), maxUtf8Length.value());
            finishGetUsing(methodBuilder, arrayFieldModel.getUsing);
        }

        private void finishGetUsing(MethodSpec.Builder methodBuilder, Method get) {
            returnNotNullGetUsing(methodBuilder);
            nullGetBranch(methodBuilder, get);
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (!nullable())
                checkArgumentNotNull(methodBuilder);
            genSet(valueBuilder, methodBuilder, varName());
        }

        private void genSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
                            Object toSet) {
            int byteOffset = verifiedByteOffset(valueBuilder);
            String endName = "__end" + name;
            methodBuilder.addStatement("long $N = bs.writeUtf8Limited(offset + $L, $N, $L)",
                    endName, byteOffset, toSet, maxUtf8Length.value());
            methodBuilder.addStatement("bs.zeroOut($N, offset + $L)",
                    endName, byteOffset + sizeInBytes());
        }

        @Override
        public void generateArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (!nullable())
                checkArgumentNotNull(methodBuilder);
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder, varName());
        }

        private void genArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder, Object toSet) {
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            String endName = "__end" + name;
            methodBuilder.addStatement(
                    "long $N = bs.writeUtf8Limited(offset + $L + elementOffset, $N, $L)",
                    endName, arrayByteOffset, toSet, maxUtf8Length.value());
            methodBuilder.addStatement("bs.zeroOut($N, offset + $L + elementOffset + $L)",
                    endName, arrayByteOffset, sizeInBytes());
        }

        @Override
        public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (getUsing != null) {
                if (!nullable()) {
                    methodBuilder.addStatement("from.$N($N)", getUsing.getName(),
                            cachedStringBuilder());
                    genSet(valueBuilder, methodBuilder, cachedStringBuilder());
                } else {
                    String getUsingResult = format("from.%s(%s)", getUsing.getName(),
                            cachedStringBuilder());
                    genSet(valueBuilder, methodBuilder, getUsingResult);
                }
            } else {
                genSet(valueBuilder, methodBuilder, format("from.%s()", get.getName()));
            }
        }

        @Override
        public void generateArrayElementCopyFrom(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            Method getUsing = arrayFieldModel.getUsing;
            if (getUsing != null) {
                if (!nullable()) {
                    methodBuilder.addStatement("from.$N(index, $N)",
                            getUsing.getName(), cachedStringBuilder());
                    genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                            cachedStringBuilder());
                } else {
                    String getUsingResult = format("from.%s(index, %s)", getUsing.getName(),
                            cachedStringBuilder());
                    genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                            getUsingResult);
                }
            } else {
                genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                        format("from.%s(index)", arrayFieldModel.get.getName()));
            }
        }

        @Override
        void generateWriteMarshallable(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedStringBuilder(valueBuilder, methodBuilder);
            finishWriteMarshallable(methodBuilder);
        }

        @Override
        void generateArrayElementWriteMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedStringBuilder(arrayFieldModel, valueBuilder, methodBuilder);
            finishWriteMarshallable(methodBuilder);
        }

        private void finishWriteMarshallable(MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("bytes.writeUtf8($N)", cachedStringBuilder());
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("bytes.writeUtf8(null)");
            methodBuilder.endControlFlow();
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$N(bytes.readUtf8($N) ? $N : null)",
                    set.getName(), cachedStringBuilder(), cachedBuilderToSettable());
        }

        @Override
        void generateArrayElementReadMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$N(index, bytes.readUtf8($N) ? $N : null)",
                    arrayFieldModel.set.getName(), cachedStringBuilder(),
                    cachedBuilderToSettable());
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int byteOffset = verifiedByteOffset(valueBuilder);
            if (getUsing != null) {
                if (!nullable()) {
                    methodBuilder.addStatement("other.$N($N)",
                            getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addCode("if ($N.length() > $L) return false;\n",
                            cachedStringBuilder(), maxUtf8Length.value());
                    methodBuilder.addCode(
                            "if (!bs.compareUtf8(offset + $L, $N)) return false;\n",
                            byteOffset, cachedStringBuilder());
                } else {
                    String localName = "__other" + name;
                    methodBuilder.addStatement("$T $N = other.$N($N)",
                            CharSequence.class, localName, getUsing.getName(),
                            cachedStringBuilder());
                    methodBuilder.addCode("if ($N != null && $N.length() > $L) return false;\n",
                            localName, localName, maxUtf8Length.value());
                    methodBuilder.addCode(
                            "if (!bs.compareUtf8(offset + $L, $N)) return false;\n",
                            byteOffset, localName);
                }
            } else {
                String localName = "__other" + name;
                methodBuilder.addStatement("$T $N = other.$N()",
                        CharSequence.class, localName, get.getName());
                methodBuilder.addCode("if ($N != null && $N.length() > $L) return false;\n",
                        localName, localName, maxUtf8Length.value());
                methodBuilder.addCode(
                        "if (!bs.compareUtf8(offset + $L, $N)) return false;\n",
                        byteOffset, localName);
            }
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            Method getUsing = arrayFieldModel.getUsing;
            if (getUsing != null) {
                if (!nullable()) {
                    methodBuilder.addStatement("other.$N(index, $N)",
                            getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addCode("if ($N.length() > $L) return false;\n",
                            cachedStringBuilder(), maxUtf8Length.value());
                    methodBuilder.addCode(
                            "if (!bs.compareUtf8(offset + $L + elementOffset, $N)) return false;\n",
                            arrayByteOffset, cachedStringBuilder());
                } else {
                    String localName = "__other" + name;
                    methodBuilder.addStatement("$T $N = other.$N(index, $N)",
                            CharSequence.class, localName, getUsing.getName(),
                            cachedStringBuilder());
                    methodBuilder.addCode("if ($N != null && $N.length() > $L) return false;\n",
                            localName, localName, maxUtf8Length.value());
                    methodBuilder.addCode(
                            "if (!bs.compareUtf8(offset + $L + elementOffset, $N)) return false;\n",
                            arrayByteOffset, localName);
                }
            } else {
                String localName = "__other" + name;
                methodBuilder.addStatement("$T $N = other.$N(index)",
                        CharSequence.class, localName, arrayFieldModel.get.getName());
                methodBuilder.addCode("if ($N != null && $N.length() > $L) return false;\n",
                        localName, localName, maxUtf8Length.value());
                methodBuilder.addCode(
                        "if (!bs.compareUtf8(offset + $L + elementOffset, $N)) return false;\n",
                        arrayByteOffset, localName);
            }
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String hashCodeCharSequenceName = "__hashCode" + name;
            methodBuilder.addStatement("$T $N = null",
                    CharSequence.class, hashCodeCharSequenceName);
            initCachedStringBuilder(valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N = $N", hashCodeCharSequenceName, cachedStringBuilder());
            methodBuilder.endControlFlow();
            return "net.openhft.chronicle.values.CharSequences.hashCode(" +
                    hashCodeCharSequenceName + ")";
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String hashCodeCharSequenceName = "__hashCode" + name;
            methodBuilder.addStatement("$T $N = null",
                    CharSequence.class, hashCodeCharSequenceName);
            initArrayElementCachedStringBuilder(arrayFieldModel, valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N = $N", hashCodeCharSequenceName, cachedStringBuilder());
            methodBuilder.endControlFlow();
            return "net.openhft.chronicle.values.CharSequences.hashCode(" +
                    hashCodeCharSequenceName + ")";
        }

        @Override
        void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            initCachedStringBuilder(valueBuilder, methodBuilder);
            genToString(methodBuilder, cachedStringBuilder());
            methodBuilder.nextControlFlow("else");
            genToString(methodBuilder, "(String) null");
            methodBuilder.endControlFlow();
        }

        @Override
        void generateArrayElementToString(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            initArrayElementCachedStringBuilder(arrayFieldModel, valueBuilder, methodBuilder);
            genArrayElementToString(methodBuilder, cachedStringBuilder());
            methodBuilder.nextControlFlow("else");
            genArrayElementToString(methodBuilder, "(String) null");
            methodBuilder.endControlFlow();
        }
    };

    private void throwNullableGetUsingVoidReturn() {
        throw new IllegalStateException(name + " field nullable " +
                get.getName() + "() shouldn't return void, because null value is " +
                "indistinguishable from empty string. Specify the parameter in " +
                set.getName() + " method as @NotNull");
    }

    private void nullGetBranch(MethodSpec.Builder methodBuilder, Method get) {
        methodBuilder.nextControlFlow("else");
        if (nullable()) {
            if (get.getReturnType() != void.class) {
                methodBuilder.addStatement("return null");
            } else {
                throwNullableGetUsingVoidReturn();
            }
        } else {
            methodBuilder.addStatement("throw new $T($S)",
                    IllegalStateException.class, name + " shouldn't be null");
        }
        methodBuilder.endControlFlow();
    }

    private boolean nullable() {
        return nullability.nullability() == NULLABLE;
    }

    private void returnNotNullGetUsing(MethodSpec.Builder methodBuilder) {
        if (getUsing.getReturnType() == String.class) {
            methodBuilder.addStatement("return $N.toString()", usingName());
        } else if (getUsing.getReturnType() != void.class) {
            methodBuilder.addStatement("return $N", usingName());
        }
    }

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    private void checkHeapArgument(MethodSpec.Builder methodBuilder) {
        if (!nullable())
            checkArgumentNotNull(methodBuilder);
        // Don't check the UTF-8 length, because this is an operation with linear complexity,
        // while it adds only little extra safety - too long string will be found only when
        // copied to native impl
    }

    private final MemberGenerator stringHeapGenerator = new ObjectHeapMemberGenerator(this) {

        @Override
        void generateFields(ValueBuilder valueBuilder) {
            field = FieldSpec.builder(String.class, fieldName(), PRIVATE)
                    .initializer("$S", "")
                    .build();
            valueBuilder.typeBuilder.addField(field);
            addCachedStringBuilder(valueBuilder);
        }

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            super.generateArrayElementFields(arrayFieldModel, valueBuilder);
            MethodSpec.Builder constructor = valueBuilder.defaultConstructorBuilder();
            constructor.beginControlFlow("for (int index = 0; index < $L; index++)",
                    arrayFieldModel.array.length());
            {
                constructor.addStatement("$N[index] = $S", field, "");
            }
            constructor.endControlFlow();

            addCachedStringBuilder(valueBuilder);
        }

        @Override
        public void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$N.setLength(0)", usingName());
            if (nullable()) {
                methodBuilder.beginControlFlow("if ($N != null)", field);
            }
            methodBuilder.addStatement("$N.append($N)", usingName(), field);
            returnNotNullGetUsing(methodBuilder);
            if (nullable()) {
                nullGetBranch(methodBuilder, getUsing);
            }
        }

        @Override
        public void generateArrayElementGetUsing(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$N.setLength(0)", usingName());
            if (nullable()) {
                methodBuilder.beginControlFlow("if ($N[index] != null)", field);
            }
            methodBuilder.addStatement("$N.append($N[index])", usingName(), field);
            returnNotNullGetUsing(methodBuilder);
            if (nullable()) {
                nullGetBranch(methodBuilder, arrayFieldModel.getUsing);
            }
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            checkHeapArgument(methodBuilder);
            methodBuilder.addStatement("this.$N = $N", field, varName());
        }

        @Override
        public void generateArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            checkHeapArgument(methodBuilder);
            methodBuilder.addStatement("this.$N[index] = $N", field, varName());
        }

        @Override
        public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (get != null) {
                methodBuilder.addStatement("this.$N(from.$N())", set.getName(), get.getName());
            } else {
                if (!nullable()) {
                    methodBuilder.addStatement(
                            "from.$N($N)", getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addStatement(
                            "this.$N($N.toString())", set.getName(), cachedStringBuilder());
                } else {
                    String getUsingResult =
                            format("from.%s(%s)", getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addStatement("$T $N = $N",
                            CharSequence.class, varName(), getUsingResult);
                    methodBuilder.addStatement("$N = $N != null ? $N.toString() : null",
                            field, varName(), cachedStringBuilder());
                }
            }
        }

        @Override
        public void generateArrayElementCopyFrom(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (get != null) {
                methodBuilder.addStatement(
                        "this.$N(index, from.$N(index))", set.getName(), get.getName());
            } else {
                if (getUsing.getReturnType() == void.class) {
                    methodBuilder.addStatement(
                            "from.$N(index, $N)", getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addStatement(
                            "this.$N(index, $N.toString())", set.getName(), cachedStringBuilder());
                } else {
                    String getUsingResult =
                            format("from.%s(index, %s)", getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addStatement("$T $N = $N",
                            CharSequence.class, varName(), getUsingResult);
                    methodBuilder.addStatement("$N[index] = $N != null ? $N.toString() : null",
                            field, varName(), cachedStringBuilder());
                }
            }
        }

        @Override
        void generateWriteMarshallable(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("bytes.writeUtf8($N)", fieldName());
        }

        @Override
        void generateArrayElementWriteMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("bytes.writeUtf8($N[index])", fieldName());
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("this.$N(bytes.readUtf8($N) ? $N.toString() : null)",
                    set.getName(), cachedStringBuilder(), cachedStringBuilder());
        }

        @Override
        void generateArrayElementReadMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("this.$N(index, bytes.readUtf8($N) ? $N.toString() : null)",
                    set.getName(), cachedStringBuilder(), cachedStringBuilder());
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (get != null) {
                boolean hasGetUsing = getUsing != null;
                if (hasGetUsing) {
                    ClassName heapClassName = valueBuilder.className();
                    methodBuilder.beginControlFlow("if (other instanceof $T)", heapClassName);
                }
                methodBuilder.addCode("if (!$T.equals($N, other.$N())) return false;\n",
                        CharSequences.class, field, get.getName());
                if (hasGetUsing) {
                    methodBuilder.nextControlFlow("else");
                    {
                        equalsWithGetUsing(methodBuilder);
                    }
                    methodBuilder.endControlFlow();
                }
            } else {
                equalsWithGetUsing(methodBuilder);
            }
        }

        private void equalsWithGetUsing(MethodSpec.Builder methodBuilder) {
            if (getUsing.getReturnType() == void.class) {
                methodBuilder.addStatement("other.$N($N)",
                        getUsing.getName(), cachedStringBuilder());
                methodBuilder.addCode("if (!$T.equals($N, $N)) return false;\n",
                        CharSequences.class, field, cachedStringBuilder());
            } else {
                methodBuilder.addCode("if (!$T.equals($N, other.$N($N))) return false;\n",
                        CharSequences.class, field, getUsing.getName(), cachedStringBuilder());
            }
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (get != null) {
                boolean hasGetUsing = getUsing != null;
                if (hasGetUsing) {
                    ClassName heapClassName = valueBuilder.className();
                    methodBuilder.beginControlFlow("if (other instanceof $T)", heapClassName);
                }
                methodBuilder.addCode("if (!$T.equals($N[index], other.$N(index))) return false;\n",
                        CharSequences.class, field, get.getName());
                if (hasGetUsing) {
                    methodBuilder.nextControlFlow("else");
                    {
                        equalsArrayElementWithGetUsing(methodBuilder);
                    }
                    methodBuilder.endControlFlow();
                }
            } else {
                equalsArrayElementWithGetUsing(methodBuilder);
            }
        }

        private void equalsArrayElementWithGetUsing(MethodSpec.Builder methodBuilder) {
            if (getUsing.getReturnType() == void.class) {
                methodBuilder.addStatement("other.$N(index, $N)",
                        getUsing.getName(), cachedStringBuilder());
                methodBuilder.addCode("if (!$T.equals($N[index], $N)) return false;\n",
                        CharSequences.class, field, cachedStringBuilder());
            } else {
                methodBuilder.addCode(
                        "if (!$T.equals($N[index], other.$N(index, $N))) return false;\n",
                        CharSequences.class, field, getUsing.getName(), cachedStringBuilder());
            }
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            return "net.openhft.chronicle.values.CharSequences.hashCode(" + field.name + ")";
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            return "net.openhft.chronicle.values.CharSequences.hashCode(" + field.name + "[index])";
        }
    };


    private final MemberGenerator charSequenceHeapGenerator = new ObjectHeapMemberGenerator(this) {

        private String isNull() {
            return fieldName() + "IsNull";
        }

        private void addCachedStringBuilderForCharSequenceHeapGenerator(ValueBuilder valueBuilder) {
            if (get == null) {
                // needed only when there is no get method, for equals()
                addCachedStringBuilder(valueBuilder);
            }
        }

        @Override
        void generateFields(ValueBuilder valueBuilder) {
            field = FieldSpec
                    .builder(StringBuilder.class, fieldName(), PRIVATE, FINAL)
                    .initializer("new $T($L)", StringBuilder.class, maxUtf8Length.value())
                    .build();
            valueBuilder.typeBuilder.addField(field);
            if (nullable()) {
                FieldSpec isNullField = FieldSpec.builder(boolean.class, isNull(), PRIVATE).build();
                valueBuilder.typeBuilder.addField(isNullField);
            }
            addCachedStringBuilderForCharSequenceHeapGenerator(valueBuilder);
        }

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            field = FieldSpec
                    .builder(ArrayTypeName.of(StringBuilder.class), fieldName(), PRIVATE, FINAL)
                    .initializer("new $T[$L]", StringBuilder.class, arrayFieldModel.array.length())
                    .build();
            valueBuilder.typeBuilder.addField(field);
            MethodSpec.Builder constructorBuilder = valueBuilder.defaultConstructorBuilder();
            constructorBuilder.beginControlFlow("for (int index = 0; index < $L; index++)");
            {
                constructorBuilder.addStatement("$N[index] = new $T($L)",
                        field, StringBuilder.class, maxUtf8Length.value());
            }
            constructorBuilder.endControlFlow();
            if (nullable()) {
                FieldSpec isNullArrayField = FieldSpec
                        .builder(ArrayTypeName.of(boolean.class), isNull(), PRIVATE, FINAL)
                        .initializer("new boolean[$L]", arrayFieldModel.array.length())
                        .build();
                valueBuilder.typeBuilder.addField(isNullArrayField);
            }
            addCachedStringBuilderForCharSequenceHeapGenerator(valueBuilder);
        }

        @Override
        public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                methodBuilder.addStatement("return !$N ? $N : null", isNull(), field);
            } else {
                methodBuilder.addStatement("return $N", field);
            }
        }

        @Override
        public void generateArrayElementGet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                methodBuilder.addStatement("return !$N[index] ? $N[index] : null", isNull(), field);
            } else {
                methodBuilder.addStatement("return $N[index]", field);
            }
        }

        @Override
        public void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (nullable())
                methodBuilder.beginControlFlow("if (!$N)", isNull());
            methodBuilder.addStatement("$N.setLength(0)", usingName());
            methodBuilder.addStatement("$N.append($N)", usingName(), field);
            returnNotNullGetUsing(methodBuilder);
            if (nullable())
                nullGetBranch(methodBuilder, getUsing);
        }

        @Override
        public void generateArrayElementGetUsing(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (nullable())
                methodBuilder.beginControlFlow("if (!$N[index])", isNull());
            methodBuilder.addStatement("$N.setLength(0)", usingName());
            methodBuilder.addStatement("$N.append($N[index])", usingName(), field);
            returnNotNullGetUsing(methodBuilder);
            if (nullable())
                nullGetBranch(methodBuilder, arrayFieldModel.getUsing);
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            checkHeapArgument(methodBuilder);
            if (nullable()) {
                methodBuilder.beginControlFlow("if ($N != null)", varName());
                {
                    methodBuilder.addStatement("$N = false", isNull());
                    methodBuilder.addStatement("$N.setLength(0)", field);
                    methodBuilder.addStatement("$N.append($N)", field, varName());
                }
                methodBuilder.nextControlFlow("else");
                {
                    methodBuilder.addStatement("$N = true", isNull());
                }
                methodBuilder.endControlFlow();
            } else {
                methodBuilder.addStatement("$N.setLength(0)", field);
                methodBuilder.addStatement("$N.append($N)", field, varName());
            }
        }

        @Override
        public void generateArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            checkHeapArgument(methodBuilder);
            if (nullable()) {
                methodBuilder.beginControlFlow("if ($N != null)", varName());
                {
                    methodBuilder.addStatement("$N[index] = false", isNull());
                    methodBuilder.addStatement("$N[index].setLength(0)", field);
                    methodBuilder.addStatement("$N[index].append($N)", field, varName());
                }
                methodBuilder.nextControlFlow("else");
                {
                    methodBuilder.addStatement("$N[index] = true", isNull());
                }
                methodBuilder.endControlFlow();
            } else {
                methodBuilder.addStatement("$N[index].setLength(0)", field);
                methodBuilder.addStatement("$N[index].append($N)", field, varName());
            }
        }

        @Override
        public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (get != null) {
                // if there is a getUsing() method, the shortcut copy:
                // this.setField(from.getField()) (*), when the from object is a native impl, does
                // unnecessary double contents copy: from native memory to from's cached SB,
                // then from that SB (which is returned from from.getField(), to the cached SB in
                // this heap value. To avoid this, do shortcut copy (*), only if the from object is
                // a heap impl:
                boolean hasGetUsing = getUsing != null;
                if (hasGetUsing) {
                    ClassName heapClassName = valueBuilder.className();
                    methodBuilder.beginControlFlow("if (from instanceof $T)", heapClassName);
                }
                // (*)
                methodBuilder.addStatement("this.$N(from.$N())", set.getName(), get.getName());
                if (hasGetUsing) {
                    methodBuilder.nextControlFlow("else");
                    {
                        copyFromWithGetUsing(methodBuilder);
                    }
                    methodBuilder.endControlFlow();
                }
            } else {
                copyFromWithGetUsing(methodBuilder);
            }
        }

        private void copyFromWithGetUsing(MethodSpec.Builder methodBuilder) {
            if (getUsing.getReturnType() == void.class) {
                if (!nullable()) {
                    methodBuilder.addStatement("from.$N($N)", getUsing.getName(), field);
                } else {
                    throwNullableGetUsingVoidReturn();
                }
            } else {
                String getUsingResult =
                        format("from.%s(%s)", getUsing.getName(), field.name);
                methodBuilder.addStatement("$T $N = $N",
                        CharSequence.class, varName(), getUsingResult);
                methodBuilder.addStatement("$N = $N == null", isNull(), varName());
            }
        }

        @Override
        public void generateArrayElementCopyFrom(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (get != null) {
                // if 1) type of field is not String (i. e. CharSequence or StringBuilder)
                // and 2) there is a getUsing() method
                // the shortcut copy: this.setField(from.getField()) (*), when the from object is
                // a native impl, does double contents copy: from native memory to from's cached SB,
                // then from that SB (which is returned from from.getField(), to the cached SB in
                // this heap value. To avoid this, do shortcut copy (*), only if the from object is
                // a heap impl:
                boolean nonStringWithGetUsing = getUsing != null && type != String.class;
                if (nonStringWithGetUsing) {
                    ClassName heapClassName = valueBuilder.className();
                    methodBuilder.beginControlFlow("if (from instanceof $T)", heapClassName);
                }
                // (*)
                methodBuilder.addStatement(
                        "this.$N(index, from.$N(index))", set.getName(), get.getName());
                if (nonStringWithGetUsing) {
                    methodBuilder.nextControlFlow("else");
                    {
                        arrayElementCopyFromWithGetUsing(methodBuilder);
                    }
                    methodBuilder.endControlFlow();
                }
            } else {
                arrayElementCopyFromWithGetUsing(methodBuilder);
            }
        }

        private void arrayElementCopyFromWithGetUsing(MethodSpec.Builder methodBuilder) {
            if (getUsing.getReturnType() == void.class) {
                if (!nullable()) {
                    methodBuilder.addStatement("from.$N(index, $N)", getUsing.getName(), field);
                } else {
                    throwNullableGetUsingVoidReturn();
                }
            } else {
                String getUsingResult =
                        format("from.%s(index, %s)", getUsing.getName(), field.name);
                methodBuilder.addStatement("$T $N = $N",
                        CharSequence.class, varName(), getUsingResult);
                methodBuilder.addStatement("$N[index] = $N == null", isNull(), varName());
            }
        }

        @Override
        void generateWriteMarshallable(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                methodBuilder.addStatement("bytes.writeUtf8(!$N ? $N : null)", isNull(), field);
            } else {
                methodBuilder.addStatement("bytes.writeUtf8($N)", field);
            }
        }

        @Override
        void generateArrayElementWriteMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                methodBuilder.addStatement(
                        "bytes.writeUtf8(!$N[index] ? $N[index] : null)", isNull(), field);
            } else {
                methodBuilder.addStatement("bytes.writeUtf8($N[index])", field);
            }
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                methodBuilder.addStatement("$N = !bytes.readUtf8($N)", isNull(), field);
            } else {
                methodBuilder.beginControlFlow("if (!bytes.readUtf8($N))", field);
                {
                    methodBuilder.addStatement("throw new $T($S)",
                            IllegalStateException.class, name + " shouldn't be null");
                }
                methodBuilder.endControlFlow();
            }
        }

        @Override
        void generateArrayElementReadMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                methodBuilder.addStatement(
                        "$N[index] = !bytes.readUtf8($N[index])", isNull(), field);
            } else {
                methodBuilder.beginControlFlow("if (!bytes.readUtf8($N[index]))", field);
                {
                    methodBuilder.addStatement("throw new $T($S + index + $N)",
                            IllegalStateException.class, name + " at ", " shouldn't be null");
                }
                methodBuilder.endControlFlow();
            }
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (get == null) {
                if (!nullable()) {
                    methodBuilder.addStatement("other.$N($N)",
                            getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addCode("if (!$T.equals($N, $N)) return false;\n",
                            CharSequences.class, field, cachedStringBuilder());
                } else {
                    if (getUsing.getReturnType() != void.class) {
                        methodBuilder.addCode(
                                "if (!$T.equals(!$N ? $N : null, other.$N($N))) return false;\n",
                                CharSequences.class, isNull(), field, getUsing.getName(),
                                cachedStringBuilder());
                    } else {
                        throwNullableGetUsingVoidReturn();
                    }
                }
            } else {
                methodBuilder.addCode("if (!$T.equals(this.$N(), other.$N())) return false;\n",
                        CharSequences.class, get.getName(), get.getName());
            }
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            Method getUsing = arrayFieldModel.getUsing;
            if (get == null) {
                if (!nullable()) {
                    methodBuilder.addStatement("other.$N(index, $N)",
                            getUsing.getName(), cachedStringBuilder());
                    methodBuilder.addCode("if (!$T.equals($N[index], $N)) return false;\n",
                            CharSequences.class, field, cachedStringBuilder());
                } else {
                    if (getUsing.getReturnType() != void.class) {
                        methodBuilder.addCode(
                                "if (!$T.equals(!$N[index] ? $N[index] : null, " +
                                        "other.$N(index, $N))) return false;\n",
                                CharSequences.class, isNull(), field, getUsing.getName(),
                                cachedStringBuilder());
                    } else {
                        throwNullableGetUsingVoidReturn();
                    }
                }
            } else {
                methodBuilder.addCode(
                        "if (!$T.equals(this.$N(index), other.$N(index))) return false;\n",
                        CharSequences.class, get.getName(), get.getName());
            }
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String prefix = "net.openhft.chronicle.values.CharSequences.hashCode(";
            if (nullable()) {
                return "(!" + isNull() + " ? " + prefix + field.name + ") : 0)";
            } else {
                return prefix + field.name + ")";
            }
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String prefix = "net.openhft.chronicle.values.CharSequences.hashCode(";
            if (nullable()) {
                return "(!" + isNull() + "[index] ? " + prefix + field.name + "[index]) : 0)";
            } else {
                return prefix + field.name + "[index])";
            }
        }

        @Override
        void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                genToString(methodBuilder, format("!%s ? %s : null", isNull(), field.name));
            } else {
                genToString(methodBuilder, field.name);
            }
        }

        @Override
        void generateArrayElementToString(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                String value = format("!%s[index] ? %s[index] : null", isNull(), field.name);
                genToString(methodBuilder, value);
            } else {
                genToString(methodBuilder, field.name + "[index]");
            }
        }
    };

    @Override
    MemberGenerator heapGenerator() {
        return type == String.class ? stringHeapGenerator : charSequenceHeapGenerator;
    }
}
