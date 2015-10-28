/*
 *      Copyright (C) 2015  higherfrequencytrading.com
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

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.bytes.BytesUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.openhft.chronicle.values.Nullability.NOT_NULL;
import static net.openhft.chronicle.values.Nullability.NULLABLE;

class CharSequenceFieldModel extends ScalarFieldModel {
    final FieldNullability nullability = new FieldNullability(this);
    MaxUtf8Length maxUtf8Length;

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        if (!template.regex.startsWith("getUsing"))
            super.addTypeInfo(m, template);
        nullability.addTemplate(m, template);
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

    private final MemberGenerator nativeGenerator = new MemberGenerator(this) {

        private FieldSpec cachedStringBuilder;

        @Override
        public void generateFields(ValueBuilder valueBuilder) {
            cachedStringBuilder = FieldSpec
                    .builder(StringBuilder.class, name + "Builder", PRIVATE, FINAL)
                    .initializer("new $T()", StringBuilder.class)
                    .build();
            valueBuilder.typeBuilder.addField(cachedStringBuilder);
        }

        @Override
        public void generateArrayElementFields(ValueBuilder valueBuilder) {
            generateFields(valueBuilder);
        }

        @Override
        public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.beginControlFlow("if (bs.readUtf8Limited(offset + $L, $N, $L) > 0)",
                    byteOffset, cachedStringBuilder, maxUtf8Length.value());
            finishGet(methodBuilder);
        }

        @Override
        public void generateArrayElementGet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            methodBuilder.beginControlFlow(
                    "if (bs.readUtf8Limited(offset + $L + elementOffset, $N, $L) > 0)",
                    arrayByteOffset, cachedStringBuilder, maxUtf8Length.value());
            finishGet(methodBuilder);
        }

        private void finishGet(MethodSpec.Builder methodBuilder) {
            if (type == StringBuilder.class) {
                methodBuilder.addStatement("return $N.toString()", cachedStringBuilder);
            } else {
                if (type != StringBuilder.class && type != CharSequence.class) {
                    throw new IllegalStateException("Only StringBuilder, String and CharSequence " +
                            "classes are supported, " + name + " field type is " + type);
                }
                methodBuilder.addStatement("return $N", cachedStringBuilder);
            }
            nullGetBranch(methodBuilder, get);
        }

        @Override
        public void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.beginControlFlow("if (bs.readUtf8Limited(offset + $L, $N, $L) > 0)",
                    byteOffset, usingName(), maxUtf8Length.value());
            finishGetUsing(methodBuilder);
        }

        @Override
        public void generateArrayElementGetUsing(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            methodBuilder.beginControlFlow(
                    "if (bs.readUtf8Limited(offset + $L + elementOffset, $N, $L) > 0)",
                    arrayByteOffset, usingName(), maxUtf8Length.value());
            finishGetUsing(methodBuilder);
        }

        private void finishGetUsing(MethodSpec.Builder methodBuilder) {
            returnNotNullGetUsing(methodBuilder);
            nullGetBranch(methodBuilder, getUsing);
        }

        private void nullGetBranch(MethodSpec.Builder methodBuilder, Method get) {
            methodBuilder.nextControlFlow("else");
            if (nullability.nullability() == NULLABLE) {
                if (get.getReturnType() != void.class) {
                    methodBuilder.addStatement("return null");
                } else {
                    throw new IllegalStateException(name + " field nullable " +
                            get.getName() + "() shouldn't return void, because null value is " +
                            "indistinguishable from empty string");
                }
            } else {
                methodBuilder.addStatement("throw new $T($S)",
                        IllegalStateException.class, name + " shouldn't be null");
            }
            methodBuilder.endControlFlow();
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (nullability.nullability() == NOT_NULL)
                checkArgumentNotNull(methodBuilder);
            genSet(valueBuilder, methodBuilder, varName());
        }

        private void genSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
                            Object toSet) {
            int byteOffset = verifiedByteOffset(valueBuilder);
            methodBuilder.addStatement("bs.writeUtf8Limited(offset + $L, $N, $L)",
                    byteOffset, toSet, maxUtf8Length.value());
        }

        @Override
        public void generateArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            if (nullability.nullability() == NOT_NULL)
                checkArgumentNotNull(methodBuilder);
            int arrayByteOffset = arrayFieldModel.verifiedByteOffset(valueBuilder);
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
            methodBuilder.addStatement("bs.writeUtf8Limited(offset + $L + elementOffset, $N, $L)",
                    arrayByteOffset, varName(), maxUtf8Length.value());
        }

        @Override
        public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            if (getUsing != null) {
                if (nullability.nullability() == NOT_NULL) {
                    methodBuilder.addStatement("from.$N($N)", getUsing.getName(),
                            cachedStringBuilder);
                    genSet(valueBuilder, methodBuilder, cachedStringBuilder);
                } else {
                    String getUsingResult = format("from.%s(%s)", getUsing.getName(),
                            cachedStringBuilder.name);
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
            if (getUsing != null) {
                if (nullability.nullability() == NOT_NULL) {
                    methodBuilder.addStatement("from.$N(index, $N)",
                            getUsing.getName(), cachedStringBuilder);
                    genSet(valueBuilder, methodBuilder, cachedStringBuilder);
                } else {
                    String getUsingResult = format("from.%s(index, %s)", getUsing.getName(),
                            cachedStringBuilder.name);
                    genSet(valueBuilder, methodBuilder, getUsingResult);
                }
            } else {
                genSet(valueBuilder, methodBuilder, format("from.%s(index)", get.getName()));
            }
        }
    };

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

    private final MemberGenerator heapGenerator = new ObjectHeapMemberGenerator(this) {
        @Override
        public void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$N.setLength(0)", usingName());
            methodBuilder.beginControlFlow("if ($N != null)", field);
            methodBuilder.addStatement("$N.append($N)", usingName(), field);
            returnNotNullGetUsing(methodBuilder);
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("return null");
            methodBuilder.endControlFlow();
        }

        @Override
        public void generateArrayElementGetUsing(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("$N.setLength(0)", usingName());
            methodBuilder.beginControlFlow("if ($N[index] != null)", field);
            methodBuilder.addStatement("$N.append($N[index])", usingName(), field);
            returnNotNullGetUsing(methodBuilder);
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("return null");
            methodBuilder.endControlFlow();
        }
    };

    @Override
    MemberGenerator heapGenerator() {
        return heapGenerator;
    }
}
