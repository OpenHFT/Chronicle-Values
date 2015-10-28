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

import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.core.Maths;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Date;

import static net.openhft.chronicle.values.Primitives.isPrimitiveIntegerType;
import static net.openhft.chronicle.values.Utils.roundUp;

class ArrayFieldModel extends FieldModel {

    ScalarFieldModel elemModel;
    Array array;

    @Override
    public void addLayoutInfo(Method m, MethodTemplate template) {
        super.addLayoutInfo(m, template);
        Array array = m.getAnnotation(Array.class);
        if (array != null) {
            if (this.array != null) {
                throw new IllegalStateException("@Array should be specified only once for " + name +
                        " field. Specified " + this.array + " and " + array);
            }
            if (array.length() <= 1)
                throw new IllegalStateException(array + ": length should be > 1, field " + name);
            this.array = array;
            int elementOffsetAlignment = array.elementOffsetAlignment();
            if (elementOffsetAlignment == Align.DEFAULT && !(elemModel instanceof ValueFieldModel))
                elementOffsetAlignment = Align.NO_ALIGNMENT;
            elemModel.setOffsetAlignmentExplicitly(elementOffsetAlignment);
            elemModel.dontCrossAlignment = array.elementDontCrossAlignment();
        }
    }

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        if (elemModel == null) {
            elemModel = createScalarModel(type);
            elemModel.name = "[element of " + name + "]";
        }
        elemModel.addTypeInfo(m, template);
    }

    ScalarFieldModel createScalarModel(Class type) {
        return createScalarFieldModel(type, name);
    }

    @NotNull
    static ScalarFieldModel createScalarFieldModel(Class type, String fieldName) {
        if (isPrimitiveIntegerType(type))
            return new IntegerFieldModel();
        if (type == float.class || type == double.class)
            return new FloatingFieldModel();
        if (type == boolean.class)
            return new BooleanFieldModel();
        if (Enum.class.isAssignableFrom(type))
            return new EnumFieldModel();
        if (type == Date.class)
            return new DateFieldModel();
        if (CharSequence.class.isAssignableFrom(type))
            return new CharSequenceFieldModel();
        if (type.isInterface())
            return new ValueFieldModel();
        throw new IllegalStateException(fieldName + " field type " + type + " is not supported: " +
                "not a primitive, enum, CharSequence or another value interface");
    }

    @Override
    int sizeInBits() {
        int elemSizeInBits = elemModel.sizeInBits();
        int elemBitExtent = elemBitExtent();
        int elemDontCrossBits = elemModel.dontCrossAlignmentInBits();
        if (elemBitExtent <= elemDontCrossBits) {
            // A power of 2, for fast index computation
            int elemsInOneAlignment = 1 << Maths.intLog2(elemDontCrossBits / elemBitExtent);
            return (array.length() / elemsInOneAlignment) * elemDontCrossBits +
                    ((array.length() % elemsInOneAlignment) - 1) * elemBitExtent +
                    elemSizeInBits;
        } else {
            assert elemDontCrossBits == Align.NO_ALIGNMENT : "" + elemDontCrossBits;
            return elemBitExtent * (array.length() - 1) + elemSizeInBits;
        }
    }

    int elemBitExtent() {
        return roundUp(elemModel.sizeInBits(), elemModel.offsetAlignmentInBits());
    }

    /**
     * 1, 2, 4 bits - unchanged, 3 bits - aligned to 4, otherwise aligned to a byte boundary
     */
    private int elemSizeInBits() {
        int elemSizeInBits = elemModel.sizeInBits();
        if (elemSizeInBits > 8)
            return (elemSizeInBits + 7) & ~7;
        return Maths.nextPower2(elemSizeInBits, 1);
    }

    @Override
    int offsetAlignmentInBytes() {
        int elementAlignment = elemModel.maxAlignmentInBytes();
        if (offsetAlignment == Align.DEFAULT) {
            return elementAlignment;
        }
        if (offsetAlignment == 0 && elementAlignment == 1) {
            // Special case, to avoid ISE below, because offset alignment of 1 of the element model
            // could be implicit (element is CharSequence or another value)
            return 1;
        }
        if (offsetAlignment < elementAlignment ||
                (elementAlignment > 0 && offsetAlignment % elementAlignment != 0)) {
            throw new IllegalStateException("Alignment of the array field " + name +
                    " " + offsetAlignment + " must be a multiple of it's element alignment " +
                    elementAlignment +
                    " (offset alignment is " + elemModel.offsetAlignmentInBytes() +
                    ", dontCross alignment is " + elemModel.dontCrossAlignmentInBytes());
        }
        return offsetAlignment;
    }

    @Override
    void postProcess() {
        super.postProcess();
        elemModel.postProcess();
    }

    @Override
    void checkState() {
        super.checkState();
        elemModel.checkState();
    }

    private class ArrayMemberGenerator extends MemberGenerator {
        private final MemberGenerator elemGenerator;

        private ArrayMemberGenerator(FieldModel fieldModel, MemberGenerator elemGenerator) {
            super(fieldModel);
            this.elemGenerator = elemGenerator;
        }

        @Override
        public void generateFields(ValueBuilder valueBuilder) {
            elemGenerator.generateArrayElementFields(valueBuilder);
        }

        @Override
        public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementGet(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateGetVolatile(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementGetVolatile(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementGetUsing(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementSet(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateSetVolatile(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementSetVolatile(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateSetOrdered(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementSetOrdered(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementAdd(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementAddAtomic(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateCompareAndSwap(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            elemGenerator.generateArrayElementCompareAndSwap(self(), valueBuilder, methodBuilder);
        }

        @Override
        public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.beginControlFlow("for (int index = 0; index < $N; index++)",
                    array.length());
            elemGenerator.generateArrayElementCopyFrom(self(), valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
        }
    }

    @NotNull
    private ArrayFieldModel self() {
        return ArrayFieldModel.this;
    }

    private MemberGenerator nativeGenerator;

    @Override
    MemberGenerator nativeGenerator() {
        if (nativeGenerator == null)
            nativeGenerator = new ArrayMemberGenerator(this, elemModel.nativeGenerator());
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new ArrayMemberGenerator(this, elemModel.heapGenerator());
    }

    void checkBounds(MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if (index < 0 || index >= $L)", array.length());
        methodBuilder.addStatement("throw new $T(index + $S)",
                ArrayIndexOutOfBoundsException.class,
                " is out of bounds, array length " + array.length());
        methodBuilder.endControlFlow();
    }
}
