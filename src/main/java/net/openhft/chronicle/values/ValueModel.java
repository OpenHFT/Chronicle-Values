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

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.values.Align.NO_ALIGNMENT;
import static net.openhft.chronicle.values.CompilerUtils.CACHED_COMPILER;
import static net.openhft.chronicle.values.Utils.roundUp;

public class ValueModel {

    public static final String $$NATIVE = "$$Native";
    public static final String $$HEAP = "$$Heap";
    private static ClassValue<Object> classValueModel = new ClassValue<Object>() {
        @Override
        protected Object computeValue(Class<?> valueType) {
            try {
                return CodeTemplate.createValueModel(valueType);
            } catch (Exception e) {
                return e;
            }
        }
    };
    final Class<?> valueType;
    private final Map<FieldModel, FieldData> fieldData = new HashMap<>();
    private final List<FieldModel> orderedFields;
    private final int sizeInBytes;
    private volatile Class nativeClass;
    private volatile Class heapClass;

    ValueModel(Class<?> valueType, Stream<FieldModel> fields) {
        this.valueType = valueType;
        orderedFields = new ArrayList<>();
        sizeInBytes = arrangeFields(fields);
    }

    /**
     * Returns a {@code ValueModel} for the given {@code valueType}, if the latter is a value
     * interface, or if it the heap or native implementation for some value interface, returns the
     * {@code ValueModel} for that value interface.
     *
     * @param valueType a value interface or the heap or native implementation class for some
     *                  value interface
     * @return a ValueModel for the given value interface, or if the given {@code valueType} is
     * the heap or native implementation for some value interface, returns the ValueModel of that
     * value interface
     * @throws IllegalArgumentException if the given valueType is not a <i>value interface</i>,
     *                                  or the heap or native implementation of some value interface, or the Chronicle Values library
     *                                  is not able to construct a ValueModel from this interface
     */
    public static ValueModel acquire(Class<?> valueType) {
        if (valueType.isInterface()) {
            if (CodeTemplate.NON_MODEL_TYPES.contains(valueType))
                throw notValueInterfaceOfImpl(valueType);
            Object valueModelOrException = classValueModel.get(valueType);
            if (valueModelOrException instanceof ValueModel)
                return (ValueModel) valueModelOrException;
            throw new IllegalArgumentException((Exception) valueModelOrException);
        }
        return doSomethingForInterfaceOr(valueType, ValueModel::acquire,
                () -> {
                    throw notValueInterfaceOfImpl(valueType);
                });
    }

    private static IllegalArgumentException notValueInterfaceOfImpl(Class<?> valueType) {
        return new IllegalArgumentException(valueType + " is not an interface nor " +
                "a generated class native or heap class");
    }

    private static <T> T doSomethingForInterfaceOr(
            Class<?> valueType, Function<Class, T> actionForInterface, Supplier<T> ifNotFound) {
        String typeName = valueType.getName();
        if (typeName.endsWith($$NATIVE) || typeName.endsWith($$HEAP)) {
            Type[] superInterfaces = valueType.getGenericInterfaces();
            for (Type superInterface : superInterfaces) {
                Class rawInterface = rawInterface(superInterface);
                // index of first $ in Foo$$Heap or Foo$$Native
                int firstDollarIndex = typeName.lastIndexOf('$') - 1;
                if (rawInterface.getName().equals(typeName.substring(0, firstDollarIndex)))
                    return actionForInterface.apply(rawInterface);
            }
        }
        return ifNotFound.get();
    }

    static boolean isValueInterfaceOrImplClass(Class<?> valueType) {
        if (valueType.isInterface()) {
            if (CodeTemplate.NON_MODEL_TYPES.contains(valueType))
                return false;
            Object valueModelOrException = classValueModel.get(valueType);
            return valueModelOrException instanceof ValueModel;
        }
        return doSomethingForInterfaceOr(valueType, ValueModel::isValueInterfaceOrImplClass,
                () -> false);
    }

    static Class rawInterface(Type superInterface) {
        if (superInterface instanceof Class) {
            return (Class) superInterface;
        } else {
            if (superInterface instanceof ParameterizedType) {
                return (Class) ((ParameterizedType) superInterface).getRawType();
            } else {
                throw new AssertionError("Super interface should be a raw interface or" +
                        "a parameterized interface");
            }
        }
    }

    private static boolean dontCross(int from, int size, int alignment) {
        return alignment == NO_ALIGNMENT || from / alignment == (from + size - 1) / alignment;
    }

    static String simpleName(Class<?> type) {
        String name = type.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * Greedy algorithm, tries to arrange most coarse-aligned fields (if equally aligned,
     * biggest) first, if holes appear due to alignment, tries to fill holes (from smallest to
     * biggest) on each step.
     * <p>
     * <p>Sure this is a suboptimal algorithm, optimal algorithm is NP hard and rather complex
     * (unless try all combinations), but the user could always arrange the fields by hand,
     * providing @Group annotation to each field.
     *
     * @return value size in bytes
     */
    private int arrangeFields(Stream<FieldModel> fields) {
        TreeMap<Long, List<FieldModel>> fieldGroups =
                fields.collect(groupingBy(f -> f.groupOrder, TreeMap::new, toList()));
        // Global watermark across field groups, doesn't let fields from higher groups go earlier
        // than any fields from lower groups
        int watermark = 0;
        Map<Integer, FieldModel> fieldEnds = new HashMap<>();
        for (Map.Entry<Long, List<FieldModel>> e : fieldGroups.entrySet()) {
            List<FieldModel> groupFields = e.getValue();
            groupFields.sort(
                    comparing(FieldModel::maxAlignmentInBytes)
                            .thenComparing(FieldModel::sizeInBits)
                            .reversed());
            // Preserve holes to be sorted from smallest to highest, to fill smallest
            // by the subsequent fields
            TreeSet<BitRange> holes = new TreeSet<>(comparing(BitRange::size).reversed());
            iterFields:
            for (FieldModel field : groupFields) {
                int fieldOffsetAlignment = field.offsetAlignmentInBits();
                int fieldDontCrossAlignment = field.dontCrossAlignmentInBits();
                int fieldSize = field.sizeInBits();
                // Try to fill a hole first
                for (BitRange hole : holes) {
                    int fieldStartInHole = roundUp(hole.from, fieldOffsetAlignment);
                    int fieldEndInHole = fieldStartInHole + fieldSize;
                    if ((fieldEndInHole < hole.to) &&
                            dontCross(fieldStartInHole, fieldSize, fieldDontCrossAlignment)) {
                        fieldData.put(field, new FieldData(fieldStartInHole, fieldSize));
                        orderedFields.add(field);
                        fieldEnds.put(fieldEndInHole, field);
                        holes.remove(hole);
                        if (hole.from != fieldStartInHole)
                            holes.add(new BitRange(hole.from, fieldStartInHole));
                        if (fieldEndInHole != hole.to)
                            holes.add(new BitRange(fieldEndInHole, hole.to));
                        continue iterFields;
                    }
                }
                // Update watermark
                int fieldStart = roundUp(watermark, fieldOffsetAlignment);
                if (!dontCross(fieldStart, fieldSize, fieldDontCrossAlignment)) {
                    assert fieldDontCrossAlignment != NO_ALIGNMENT;
                    fieldStart = roundUp(watermark, fieldDontCrossAlignment);
                    assert dontCross(fieldStart, fieldSize, fieldDontCrossAlignment);
                }
                fieldData.put(field, new FieldData(fieldStart, fieldSize));
                orderedFields.add(field);
                int fieldEnd = fieldStart + fieldSize;
                fieldEnds.put(fieldEnd, field);
                if (fieldStart > watermark)
                    holes.add(new BitRange(watermark, fieldStart));
                watermark = fieldEnd;
            }
            // Drain holes, increasing field extents
            for (BitRange hole : holes) {
                if (hole.from == 0)
                    continue;
                FieldModel fieldToExtend = fieldEnds.remove(hole.from);
                assert fieldToExtend != null;
                fieldData.get(fieldToExtend).bitExtent += hole.size();
            }
        }
        int byteRoundedWatermark = roundUp(watermark, 8);
        if (byteRoundedWatermark != watermark) {
            FieldModel lastField = fieldEnds.remove(watermark);
            assert lastField != null;
            fieldData.get(lastField).bitExtent += byteRoundedWatermark - watermark;
        }
        return byteRoundedWatermark / 8;
    }

    public Stream<FieldModel> fields() {
        return orderedFields.stream();
    }

    Class firstPrimitiveFieldType() {
        Class firstFieldType = orderedFields.get(0).type;
        if (firstFieldType.isPrimitive())
            return firstFieldType;
        return ValueModel.acquire(firstFieldType).firstPrimitiveFieldType();
    }

    /**
     * Returns the recommended alignment of a flyweight bytes offset, to satisfy alignments of all
     * the fields. It is the most coarse among all of it's fields' {@linkplain Align#offset()
     * offset} and {@linkplain Align#dontCross() don't cross} alignments.
     * <p>
     * <p>Returns a positive integer {@code >=} 1.
     *
     * @return the alignment of the flyweight value itself, to satisfy fields' alignments
     */
    public int recommendedOffsetAlignment() {
        return Math.max(fields().mapToInt(FieldModel::maxAlignmentInBytes).max().getAsInt(), 1);
    }

    public int sizeInBytes() {
        return sizeInBytes;
    }

    int fieldBitOffset(FieldModel field) {
        return fieldData.get(field).bitOffset;
    }

    /**
     * >= fieldModel.sizeInBits(), may be rounded up, if the hole in the value model is anyway empty
     */
    int fieldBitExtent(FieldModel field) {
        return fieldData.get(field).bitExtent;
    }

    /**
     * Generates (if not yet) and returns a native (flyweight) implementation for this ValueModel.
     *
     * @return a native (flyweight) implementation for this ValueModel
     * @throws ImplGenerationFailedException if generation failed
     */
    public Class nativeClass() {
        Class c;
        if ((c = nativeClass) != null)
            return c;
        synchronized (this) {
            if ((c = nativeClass) != null)
                return c;
            nativeClass = c = createNativeClass();
            return c;
        }
    }

    /**
     * Generates (if not yet) and returns a heap implementation for this ValueModel.
     *
     * @return a heap implementation for this ValueModel
     * @throws ImplGenerationFailedException if generation failed
     */
    public Class heapClass() {
        Class c;
        if ((c = heapClass) != null)
            return c;
        synchronized (this) {
            if ((c = heapClass) != null)
                return c;
            heapClass = c = createHeapClass();
            return c;
        }
    }

    private Class createNativeClass() {
        return createClass(simpleName() + $$NATIVE, Generators::generateNativeClass);
    }

    private Class createHeapClass() {
        return createClass(simpleName() + $$HEAP, Generators::generateHeapClass);
    }

    String simpleName() {
        return simpleName(valueType);
    }

    private Class createClass(
            String className, BiFunction<ValueModel, String, String> generateClass) {
        String classNameWithPackage = valueType.getPackage().getName() + "." + className;
        ClassLoader cl = BytecodeGen.getClassLoader(valueType);
        try {
            return cl.loadClass(classNameWithPackage);
        } catch (ClassNotFoundException ignored) {
            String javaCode = generateClass.apply(this, className);
            try {
                return CACHED_COMPILER.loadFromJava(valueType, cl, classNameWithPackage, javaCode);
            } catch (ClassNotFoundException e) {
                Jvm.warn().on(ValueModel.class, "Failed to compile " + e + "\n" + javaCode);
                throw new ImplGenerationFailedException(e);
            }
        }
    }

    private static class FieldData {
        int bitOffset;
        int bitExtent;

        private FieldData(int bitOffset, int bitExtent) {
            this.bitOffset = bitOffset;
            this.bitExtent = bitExtent;
        }
    }

    private static class BitRange {
        int from;
        int to;

        BitRange(int from, int to) {
            this.from = from;
            this.to = to;
        }

        int size() {
            return to - from;
        }
    }
}