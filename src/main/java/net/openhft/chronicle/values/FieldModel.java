/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

import com.squareup.javapoet.MethodSpec;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import static net.openhft.chronicle.values.Generators.methodBuilder;
import static net.openhft.chronicle.values.Utils.capitalize;

public abstract class FieldModel {
    String name;
    /**
     * The field type if this is a {@link ScalarFieldModel},
     * elem type if this is a {@link ArrayFieldModel}
     */
    Class type;
    long groupOrder = 0;
    boolean alignmentSpecifiedExplicitly;
    int offsetAlignment;
    int dontCrossAlignment;

    Method get;
    Method getVolatile;
    Method getUsing;
    Method set;
    Method setVolatile;
    Method setOrdered;
    Method add;
    Method addAtomic;
    Method compareAndSwap;

    void addLayoutInfo(Method m, MethodTemplate template) {
        Group group = m.getAnnotation(Group.class);
        if (group != null) {
            // this offset makes default groupOrder=0 always smaller than specified order
            long offset = 1L << 32;
            this.groupOrder = offset + group.value();
        }
        Align align = m.getAnnotation(Align.class);
        if (align != null) {
            // if both specified
            if (align.offset() > 0 && align.dontCross() >= 0 && align.dontCross() % align.offset() != 0) {
                    throw new IllegalStateException(align + " dontCross alignment should be " +
                            "a multiple of offset alignment, field " + name);
            }
            setOffsetAlignmentExplicitly(align.offset());
            dontCrossAlignment = align.dontCross();
        }
    }

    void setOffsetAlignmentExplicitly(int offsetAlignment) {
        if (alignmentSpecifiedExplicitly) {
            throw new IllegalStateException("Alignment for the field " + name +
                    " should be specified only once");
        }
        alignmentSpecifiedExplicitly = true;
        this.offsetAlignment = offsetAlignment;
    }

    void addTypeInfo(Method m, MethodTemplate template) {
        Class fieldType = template.fieldType.apply(m);
        if (type != null && type != fieldType) {
            throw new IllegalStateException("different field types in methods of the field " +
                    name + ": " + type + " " + fieldType);
        }
        type = fieldType;
    }

    final void addInfo(Method m, MethodTemplate template) {
        addTypeInfo(m, template);
        addLayoutInfo(m, template);
    }

    abstract int sizeInBits();

    final int sizeInBytes() {
        int sizeInBits = sizeInBits();
        assert sizeInBits % 8 == 0;
        return sizeInBits / 8;
    }

    abstract int offsetAlignmentInBytes();

    int dontCrossAlignmentInBytes() {
        if (dontCrossAlignment == Align.DEFAULT)
            return Align.NO_ALIGNMENT;
        return dontCrossAlignment;
    }

    /**
     * Maximum of {@link #offsetAlignmentInBytes()} and {@link #dontCrossAlignmentInBytes()}
     */
    final int maxAlignmentInBytes() {
        return Math.max(offsetAlignmentInBytes(), dontCrossAlignmentInBytes());
    }

    final int offsetAlignmentInBits() {
        int offset = offsetAlignmentInBytes();
        return offset > 0 ? offset * 8 : 1;
    }

    final int dontCrossAlignmentInBits() {
        return dontCrossAlignmentInBytes() * 8;
    }

    /**
     * Should be called after method processing, but before sizeInBytes/alignment queries and
     * members generation
     */
    void postProcess() {
    }

    void checkState() {
        checkDontCrossMultipleOfOffsetAlignment();
        checkDontCrossSmallerThanSize();
    }

    void checkAnyWriteMethodPresent() {
        if (set == null && setVolatile == null && setOrdered == null && add == null &&
                addAtomic == null && compareAndSwap == null) {
            throw new IllegalStateException("Some writing method should be present for field " +
                    name);
        }
    }

    void checkDontCrossMultipleOfOffsetAlignment() {
        int offset = offsetAlignmentInBytes();
        int dontCross = dontCrossAlignmentInBytes();
        if (offset != 0 && dontCross % offset != 0) {
            throw new IllegalStateException("offset alignment " + offset + "should be a multiple " +
                    "of dontCross alignment " + dontCross + ", field " + name);
        }
    }

    void checkDontCrossSmallerThanSize() {
        int dontCross = dontCrossAlignmentInBits();
        if (dontCross != Align.NO_ALIGNMENT && dontCross < sizeInBits()) {
            throw new IllegalStateException("dontCross alignment should be wider than the field " +
                    name + " itself");
        }
    }

    MemberGenerator nativeGenerator() {
        throw new UnsupportedOperationException(getClass() + "");
    }

    MemberGenerator createHeapGenerator() {
        throw new UnsupportedOperationException(getClass() + "");
    }

    private MemberGenerator heapGenerator;

    MemberGenerator heapGenerator() {
        if (heapGenerator == null)
            heapGenerator = createHeapGenerator();
        return heapGenerator;
    }

    void generateNativeMembers(ValueBuilder valueBuilder) {
        generateMembers(nativeGenerator(), valueBuilder);
    }

    void generateHeapMembers(ValueBuilder valueBuilder) {
        generateMembers(heapGenerator(), valueBuilder);
    }

    void generateMembers(MemberGenerator generator, ValueBuilder valueBuilder) {
        generator.generateFields(valueBuilder);
        generateMethod(valueBuilder, get, generator::generateGet);
        generateMethod(valueBuilder, getVolatile, generator::generateGetVolatile);
        generateMethod(valueBuilder, getUsing, generator::generateGetUsing, usingName());
        generateMethod(valueBuilder, set, generator::generateSet, varName());
        generateMethod(valueBuilder, setVolatile, generator::generateSetVolatile, varName());
        generateMethod(valueBuilder, setOrdered, generator::generateSetOrdered, varName());
        generateMethod(valueBuilder, add, generator::generateAdd, "addition");
        generateMethod(valueBuilder, addAtomic, generator::generateAddAtomic, "addition");
        generateMethod(valueBuilder, compareAndSwap, generator::generateCompareAndSwap,
                oldName(), newName());
    }

    public String name() {
        return name;
    }

    /**
     * Field name as variable name. Not equal to field name, because it could clash with Java
     * keyword or type name, e. g. getInt()/setInt()
     */
    String varName() {
        return "_" + name;
    }

    public String fieldName() {
        return "__field" + name;
    }

    String usingName() {
        return "using" + capitalize(name);
    }

    String oldName() {
        return "old" + capitalize(name);
    }

    String newName() {
        return "new" + capitalize(name);
    }

    String capTypeName() {
        return capitalize(type.getName());
    }

    String readMethod() {
        if (type == char.class) {
            return "readUnsignedShort";
        } else {
            return "read" + capTypeName();
        }
    }

    String writeMethod() {
        if (type == char.class) {
            return "writeUnsignedShort";
        } else {
            return "write" + capTypeName();
        }
    }

    private void generateMethod(
            ValueBuilder valueBuilder, Method m,
            BiConsumer<ValueBuilder, MethodSpec.Builder> generate, String... parameterNames) {
        if (m != null) {
            List<String> paramNames = new ArrayList<>();
            if (this instanceof ArrayFieldModel)
                paramNames.add("index");
            paramNames.addAll(Arrays.asList(parameterNames));

            MethodSpec.Builder methodBuilder = methodBuilder(m, paramNames);
            generate.accept(valueBuilder, methodBuilder);
            valueBuilder.typeBuilder.addMethod(methodBuilder.build());
        }
    }

    void setGet(Method get) {
        if (this.get != null) {
            throw new IllegalStateException("Get method is already declared for the field " + name +
                    ": " + this.get.getName() + ", " + get.getName());
        }
        this.get = get;
    }

    public Method get() {
        return get;
    }

    void setGetVolatile(Method getVolatile) {
        if (this.getVolatile != null) {
            throw new IllegalStateException("GetVolatile is already declared for the field" +
                    name + ": " + this.getVolatile.getName() + ", " + getVolatile.getName());
        }
        this.getVolatile = getVolatile;
    }

    void setGetUsing(Method getUsing) {
        if (this.getUsing != null) {
            throw new IllegalStateException("GetUsing is already declared for the field " +
                    name + ": " + this.getUsing.getName() + ", " + getUsing.getName());
        }
        this.getUsing = getUsing;
    }

    void setSet(Method set) {
        if (this.set != null) {
            throw new IllegalStateException("Set method is already declared for the field " + name +
                    ": " + this.set.getName() + ", " + set.getName());
        }
        this.set = set;
    }

    public Method set() {
        return set;
    }

    public Method setOrSetOrderedOrSetVolatile() {
        if (set != null)
            return set;
        if (setOrdered != null)
            return setOrdered;
        if (setVolatile != null)
            return setVolatile;
        throw new IllegalStateException("set or setVolatile or setOrdered expected for field " +
                name);
    }

    void setSetVolatile(Method setVolatile) {
        if (this.setVolatile != null) {
            throw new IllegalStateException("SetVolatile is already declared for the field " +
                    name + ": " + this.setVolatile.getName() + ", " + setVolatile.getName());
        }
        this.setVolatile = setVolatile;
    }

    void setSetOrdered(Method setOrdered) {
        if (this.setOrdered != null) {
            throw new IllegalStateException("SetOrdered is already declared for the field " +
                    name + ": " + this.setOrdered.getName() + ", " + setOrdered.getName());
        }
        this.setOrdered = setOrdered;
    }

    void setAdd(Method add) {
        if (this.add != null) {
            throw new IllegalStateException("Add method is already declared for the field " + name +
                    ": " + this.add.getName() + ", " + add.getName());
        }
        this.add = add;
    }

    void setAddAtomic(Method addAtomic) {
        if (this.addAtomic != null) {
            throw new IllegalStateException("AddAtomic is already declared for the field " +
                    name + ": " + this.addAtomic.getName() + ", " + addAtomic.getName());
        }
        this.addAtomic = addAtomic;
    }

    void setCompareAndSwap(Method compareAndSwap) {
        if (this.compareAndSwap != null) {
            throw new IllegalStateException(
                    "CompareAndSwap is already declared for the field " +
                    name + ": " + this.compareAndSwap.getName() + ", " + compareAndSwap.getName());
        }
        this.compareAndSwap = compareAndSwap;
    }

    int verifiedByteOffset(ValueBuilder valueBuilder) {
        int bitOffset = valueBuilder.model.fieldBitOffset(this);
        assert bitOffset % 8 == 0 :
                getClass().getSimpleName() + " " + name + " should be byte-aligned";
        return bitOffset / 8;
    }

    void checkArgumentNotNull(MethodSpec.Builder builder) {
        builder.beginControlFlow("if ($N == null)", varName());
        builder.addStatement("throw new $T($S)",
                IllegalArgumentException.class, name + " shouldn't be null");
        builder.endControlFlow();
    }

    static void genVerifiedElementOffset(
            ArrayFieldModel arrayField, MethodSpec.Builder methodBuilder) {
        int elemBitExtent = arrayField.elemBitExtent();
        assert elemBitExtent % 8 == 0;
        int elemByteExtent = elemBitExtent / 8;
        methodBuilder.addStatement("long elementOffset = index * $LL", elemByteExtent);
    }

    public Method getOrGetVolatile() {
        if (get != null)
            return get;
        if (getVolatile != null)
            return getVolatile;
        throw new IllegalStateException("get or getVolatile expected for field " + name);
    }
}
