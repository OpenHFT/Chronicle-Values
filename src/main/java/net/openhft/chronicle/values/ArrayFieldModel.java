/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.core.Maths;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static net.openhft.chronicle.values.Utils.roundUp;

/**
 * Metadata model for an array field.
 *
 * <p>The model pairs a {@link ScalarFieldModel} describing the element type
 * with the {@link Array} annotation found on the value interface. The
 * annotation supplies the fixed {@linkplain Array#length() length} and the
 * optional alignment rules for individual elements.</p>
 *
 * <p>The declared length is used when generating loops and bounds checks and
 * contributes directly to the size reported by {@link #sizeInBits()}. Any
 * element offset alignment and <em>dont-cross</em> boundary specified via
 * {@link Array#elementOffsetAlignment()} and
 * {@link Array#elementDontCrossAlignment()} are applied to the element model so
 * that accessors honour these constraints. The array field alignment is then
 * derived from the element alignment in
 * {@link #offsetAlignmentInBytes()}.</p>
 *
 * <p>Code generation routines rely on this metadata to compute element
 * addresses, perform index validation and emit bulk operations such as copy or
 * marshalling loops.</p>
 *
 * <p>Example value interface accessor:</p>
 * <pre>{@code
 * interface Order {
 *     @Array(length = 8)
 *     void setPriceAt(int index, long price);
 *     long getPriceAt(int index);
 * }
 * }</pre>
 */
public class ArrayFieldModel extends FieldModel {

    /** Model of the array element type. */
    private final ScalarFieldModel elemModel;
    /** Annotation instance holding declared array properties. */
    Array array;
    private MemberGenerator nativeGenerator;

    public ArrayFieldModel(ScalarFieldModel elemModel) {
        this.elemModel = elemModel;
    }

    /**
     * Extracts {@link Array} metadata from the interface method and applies the
     * declared alignment to the element model. The method guards against
     * multiple declarations and validates that the length is greater than one.
     *
     * <p>The element offset and dont-cross constraints from the annotation are
     * propagated to {@code elemModel}. This ensures that subsequent size and
     * alignment computations use the same parameters as an equivalent scalar
     * field.</p>
     */
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

    /**
     * Returns the total storage requirement of the array in bits.
     * The calculation honours element alignment and the declared
     * {@link Array#elementDontCrossAlignment() dont-cross} boundary. The
     * generated code relies on this value when computing element offsets.
     */
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

    /**
     * Element bit extent rounded up to the element offset alignment. This value
     * is used when laying out the array so that each element starts on a
     * boundary compatible with the element model.
     */
    int elemBitExtent() {
        return roundUp(elemModel.sizeInBits(), elemModel.offsetAlignmentInBits());
    }

    /**
     * Determines the alignment of the array field itself. The result must be a
     * multiple of the element alignment. When no explicit offset is supplied the
     * element alignment is reused. Generated accessors rely on this alignment to
     * compute the base address of the array in the enclosing value.
     */
    @Override
    int offsetAlignmentInBytes() {
        int elementAlignment = elemModel.maxAlignmentInBytes();
        if (offsetAlignment == Align.DEFAULT) {
            return elementAlignment;
        }
        if (offsetAlignment == 0 && elementAlignment > 0) {
            // Special case, to avoid ISE below, because offset alignment of 1 of the element model
            // could be implicit (element is CharSequence or another value)
            return elementAlignment;
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

    @NotNull
    private ArrayFieldModel self() {
        return ArrayFieldModel.this;
    }

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

    /**
     * Emits a bounds check for element access. The generated code throws an
     * {@link ArrayIndexOutOfBoundsException} when the supplied index is outside
     * {@link Array#length()}.
     */
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

    /**
     * Delegates generation of element accessors and bulk operations.
     * The helper calls through to the element's own generator with the
     * correct index calculations. Loop constructs emitted by this class rely on
     * the fixed {@link Array#length()} recorded in the outer model.
     */
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
}
