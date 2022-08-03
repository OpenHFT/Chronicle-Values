/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
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

import com.squareup.javapoet.MethodSpec;

import static java.lang.String.format;

class BooleanFieldModel extends PrimitiveFieldModel {

    private MemberGenerator nativeGenerator = new MemberGenerator(BooleanFieldModel.this) {

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            // no fields
        }

        @Override
        void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            get(valueBuilder, methodBuilder, "");
        }

        private void get(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String readType) {
            int bitOffset = valueBuilder.model.fieldBitOffset(BooleanFieldModel.this);
            int byteOffset = bitOffset / 8;
            int bitShift = bitOffset & 7;
            methodBuilder.addStatement("return (bs.read$NByte(offset + $L) & (1 << $L)) != 0",
                    readType, byteOffset, bitShift);
        }

        @Override
        void generateArrayElementGet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayElementGet(arrayFieldModel, valueBuilder, methodBuilder, "");
        }

        private void arrayElementGet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder, String readType) {
            arrayFieldModel.checkBounds(methodBuilder);
            int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayFieldModel);
            methodBuilder.addStatement("int bitOffset = $L + index", arrayBitOffset);
            methodBuilder.addStatement("int byteOffset = bitOffset / 8");
            methodBuilder.addStatement("int bitShift = bitOffset & 7");
            methodBuilder.addStatement(
                    "return (bs.read$NByte(offset + byteOffset) & (1 << bitShift)) != 0", readType);
        }

        @Override
        void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            get(valueBuilder, methodBuilder, "Volatile");
        }

        @Override
        void generateArrayElementGetVolatile(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayElementGet(arrayFieldModel, valueBuilder, methodBuilder, "Volatile");
        }

        @Override
        void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            set(valueBuilder, methodBuilder, "", "");
        }

        private void set(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
                String readType, String writeType) {
            int bitOffset = valueBuilder.model.fieldBitOffset(BooleanFieldModel.this);
            int byteOffset = bitOffset / 8;
            int bitShift = bitOffset & 7;
            methodBuilder.addStatement("int b = bs.read$NByte(offset + $L)", readType, byteOffset);
            methodBuilder.beginControlFlow("if ($N)", varName());
            methodBuilder.addStatement("b |= (1 << $L)", bitShift);
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("b &= ~(1 << $L)", bitShift);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("bs.write$NByte(offset + $L, (byte) b)",
                    writeType, byteOffset);
        }

        @Override
        void generateArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            arrayElementSet(arrayFieldModel, valueBuilder, methodBuilder, "", "");
        }

        private void arrayElementSet
                (ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                 MethodSpec.Builder methodBuilder, String readType, String writeType) {
            int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayFieldModel);
            methodBuilder.addStatement("int bitOffset = $L + index", arrayBitOffset);
            methodBuilder.addStatement("int byteOffset = bitOffset / 8");
            methodBuilder.addStatement("int bitShift = bitOffset & 7");
            methodBuilder.addStatement("int b = bs.read$NByte(offset + byteOffset)", readType);
            methodBuilder.beginControlFlow("if ($N)", varName());
            methodBuilder.addStatement("b |= (1 << bitShift)");
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("b &= ~(1 << bitShift)");
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("bs.write$NByte(offset + byteOffset, (byte) b)", writeType);
        }

        @Override
        void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            set(valueBuilder, methodBuilder, "Volatile", "Volatile");
        }

        @Override
        void generateArrayElementSetVolatile(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            arrayElementSet(arrayFieldModel, valueBuilder, methodBuilder, "Volatile", "Volatile");
        }

        @Override
        void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int bitOffset = valueBuilder.model.fieldBitOffset(BooleanFieldModel.this);
            int byteOffset = bitOffset / 8;
            methodBuilder.addStatement("long byteOffset = (offset + $L) & ~3L", byteOffset);
            methodBuilder.addStatement("int bitShift = (int) (((offset * 8) + $L) & 31)",
                    bitOffset);
            endSetOrdered(methodBuilder);
        }

        private void endSetOrdered(MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("int i = bs.readVolatileInt(byteOffset)");
            methodBuilder.beginControlFlow("if ($N)", varName());
            methodBuilder.addStatement("i |= (1 << bitShift)");
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("i &= ~(1 << bitShift)");
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("bs.writeOrderedInt(byteOffset, i)");
        }

        @Override
        void generateArrayElementSetOrdered(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayFieldModel);
            methodBuilder.addStatement("long bitOffset = (offset * 8) + $L + index",
                    arrayBitOffset);
            methodBuilder.addStatement("long byteOffset = (bitOffset / 32) * 4");
            methodBuilder.addStatement("int bitShift = (int) ((bitOffset + $L) & 31)");
            endSetOrdered(methodBuilder);
        }

        @Override
        void generateCompareAndSwap(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int bitOffset = valueBuilder.model.fieldBitOffset(BooleanFieldModel.this);
            int byteOffset = bitOffset / 8;
            methodBuilder.addStatement("long byteOffset = (offset + $L) & ~3L", byteOffset);
            methodBuilder.addStatement("int bitShift = (int) (((offset * 8) + $L) & 31)",
                    bitOffset);
            endCompareAndSwap(methodBuilder);
        }

        private void endCompareAndSwap(MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("int i = bs.readVolatileInt(byteOffset), x");
            methodBuilder.beginControlFlow("if ($N)", newName());
            methodBuilder.addStatement("x = i | (1 << bitShift)");
            methodBuilder.nextControlFlow("else");
            methodBuilder.addStatement("x = i & ~(1 << bitShift)");
            methodBuilder.endControlFlow();
            methodBuilder.beginControlFlow("if ($N == $N)", oldName(), newName());
            methodBuilder.addCode("if (i != x) return false;\n");
            methodBuilder.nextControlFlow("else");
            methodBuilder.addCode("if (i == x) return false;\n");
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("return bs.compareAndSwapInt(byteOffset, i, x)");
        }

        @Override
        void generateArrayElementCompareAndSwap(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayFieldModel);
            methodBuilder.addStatement("long bitOffset = (offset * 8) + $L + index",
                    arrayBitOffset);
            methodBuilder.addStatement("long byteOffset = (bitOffset / 32) * 4");
            methodBuilder.addStatement("int bitShift = (int) ((bitOffset + $L) & 31)");
            endCompareAndSwap(methodBuilder);
        }

        @Override
        void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String from = format("from.%s()", getOrGetVolatile().getName());
            copyFrom(valueBuilder, methodBuilder, from);
        }

        private void copyFrom(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String from) {
            if (set != null) {
                methodBuilder.addStatement("$N($N)", set.getName(), from);
            } else {
                methodBuilder.addStatement("boolean $N = $N", varName(), from);
                set(valueBuilder, methodBuilder, "", "");
            }
        }

        @Override
        void generateArrayElementCopyFrom(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String from = format("from.%s(index)", arrayFieldModel.getOrGetVolatile().getName());
            arrayElementCopyFrom(arrayFieldModel, valueBuilder, methodBuilder, from);
        }

        private void arrayElementCopyFrom(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder, String from) {
            if (arrayFieldModel.set != null) {
                methodBuilder.addStatement("$N(index, $N)", arrayFieldModel.set.getName(), from);
            } else {
                methodBuilder.addStatement("boolean $N = $N", varName(), from);
                arrayElementSet(arrayFieldModel, valueBuilder, methodBuilder, "", "");
            }
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            copyFrom(valueBuilder, methodBuilder, "bytes.readBoolean()");
        }

        @Override
        void generateArrayElementReadMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayElementCopyFrom(arrayFieldModel, valueBuilder, methodBuilder,
                    "bytes.readBoolean()");
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addCode("if ($N() != other.$N()) return false;\n",
                    getOrGetVolatile().getName(), getOrGetVolatile().getName());
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String get = arrayFieldModel.getOrGetVolatile().getName();
            methodBuilder.addCode("if ($N(index) != other.$N(index)) return false;\n", get, get);
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            return format("Boolean.hashCode(%s())", getOrGetVolatile().getName());
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            return format("Boolean.hashCode(%s(index))",
                    arrayFieldModel.getOrGetVolatile().getName());
        }
    };

    @Override
    int offsetAlignmentInBytes() {
        if (offsetAlignment == Align.DEFAULT) {
            throw new IllegalStateException("Default offset alignment doesn't make sense for " +
                    "boolean field " + name);
        }
        return offsetAlignment;
    }

    @Override
    int dontCrossAlignmentInBytes() {
        if (dontCrossAlignment == Align.DEFAULT) {
            throw new IllegalStateException("Default dontCross alignment doesn't make sense for " +
                    "boolean field " + name);
        }
        return dontCrossAlignment;
    }

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new PrimitiveBackedHeapMemberGenerator(this) {

        };
    }
}
