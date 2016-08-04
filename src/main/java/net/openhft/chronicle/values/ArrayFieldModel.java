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

import static net.openhft.chronicle.values.Utils.roundUp;

public class ArrayFieldModel extends FieldModel {

    private final ScalarFieldModel elemModel;
    Array array;

    public ArrayFieldModel(ScalarFieldModel elemModel) {
        this.elemModel = elemModel;
    }

    @Override
    void addLayoutInfo(Method m, MethodTemplate template) {
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
            elemGenerator.generateArrayElementFields(self(), valueBuilder);
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
            beginLoop(methodBuilder);
            elemGenerator.generateArrayElementCopyFrom(self(), valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
        }

        private void beginLoop(MethodSpec.Builder methodBuilder) {
            methodBuilder.beginControlFlow("for (int index = 0; index < $L; index++)",
                    array.length());
        }

        @Override
        void generateWriteMarshallable(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            beginLoop(methodBuilder);
            elemGenerator.generateArrayElementWriteMarshallable(
                    self(), valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
        }

        @Override
        void generateArrayElementWriteMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            throw new UnsupportedOperationException();
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            beginLoop(methodBuilder);
            elemGenerator.generateArrayElementReadMarshallable(self(), valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            beginLoop(methodBuilder);
            elemGenerator.generateArrayElementEquals(self(), valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
        }

        /**
         * Copies google/auto value's strategy of hash code generation
         */
        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String hashCodeVarName = varName() + "HashCode";
            methodBuilder.addStatement("int $N = 1", hashCodeVarName);
            beginLoop(methodBuilder);
            methodBuilder.addStatement("$N *= 1000003", hashCodeVarName);
            String elemHashCode = elemGenerator.generateArrayElementHashCode(
                    self(), valueBuilder, methodBuilder);
            methodBuilder.addStatement("$N ^= $N", hashCodeVarName, elemHashCode);
            methodBuilder.endControlFlow();
            return hashCodeVarName;
        }

        @Override
        void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("sb.append($S)", ", " + fieldModel.name + "=[");
            beginLoop(methodBuilder);
            elemGenerator.generateArrayElementToString(self(), valueBuilder, methodBuilder);
            methodBuilder.endControlFlow();
            methodBuilder.addStatement("sb.setCharAt(sb.length() - 2, ']')");
            methodBuilder.addStatement("sb.setLength(sb.length() - 1)");
        }

        @Override
        void generateArrayElementToString(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            throw new UnsupportedOperationException();
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

    public Array array() {
        return array;
    }
}
