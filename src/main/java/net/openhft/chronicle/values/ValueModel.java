/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.values;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.values.Align.NO_ALIGNMENT;
import static net.openhft.chronicle.values.Utils.roundUp;
import static net.openhft.compiler.CompilerUtils.CACHED_COMPILER;

public class ValueModel {

    public static final String $$NATIVE = "$$Native";
    public static final String $$HEAP = "$$Heap";

    public static ValueModel acquire(Class<?> valueType) {
        if (valueType.isInterface())
            return classValueModel.get(valueType);
        if (valueType.getName().endsWith($$NATIVE)) {
            Type[] superInterfaces = valueType.getGenericInterfaces();
            for (Type superInterface : superInterfaces) {
                Class rawInterface;
                if (superInterface instanceof Class) {
                    rawInterface = (Class) superInterface;
                } else {
                    if (superInterface instanceof ParameterizedType) {
                        rawInterface = (Class) ((ParameterizedType) superInterface).getRawType();
                    } else {
                        throw new AssertionError("Super interface should be a raw interface or" +
                                "a parameterized interface");
                    }
                }
                if (!CodeTemplate.NON_MODEL_TYPES.contains(rawInterface))
                    return classValueModel.get(rawInterface);
            }
        }
        if (valueType.getName().endsWith($$HEAP)) {
            throw new UnsupportedOperationException();
        }
        throw new IllegalArgumentException(valueType + " is not an interface nor" +
                "a generated class native or heap class");
    }

    private static ClassValue<ValueModel> classValueModel = new ClassValue<ValueModel>() {
        @Override
        protected ValueModel computeValue(Class<?> valueType) {
            return CodeTemplate.createValueModel(valueType);
        }
    };

    private static class FieldData {
        int bitOffset;
        int bitExtent;

        private FieldData(int bitOffset, int bitExtent) {
            this.bitOffset = bitOffset;
            this.bitExtent = bitExtent;
        }
    }

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

    private static class BitRange {
        int from;
        int to;

        public BitRange(int from, int to) {
            this.from = from;
            this.to = to;
        }

        int size() {
            return to - from;
        }
    }

    /**
     * Greedy algorithm, tries to arrange most coarse-aligned fields (if equally aligned,
     * biggest) first, if holes appear due to alignment, tries to fill holes (from smallest to
     * biggest) on each step.
     *
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

    private static boolean dontCross(int from, int size, int alignment) {
        return alignment == NO_ALIGNMENT || from / alignment == (from + size - 1) / alignment;
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
     *
     * <p>Returns a positive integer >= 1.
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

    Class createNativeClass() {
        return createClass(simpleName() + $$NATIVE, Generators::generateNativeClass);
    }

    Class createHeapClass() {
        return createClass(simpleName() + $$HEAP, Generators::generateHeapClass);
    }

    String simpleName() {
        return simpleName(valueType);
    }

    static String simpleName(Class<?> type) {
        String name = type.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private Class createClass(
            String className, BiFunction<ValueModel, String, String> generateClass) {
        String classNameWithPackage = valueType.getPackage().getName() + "." + className;
        ClassLoader cl = valueType.getClassLoader();
        try {
            return cl.loadClass(classNameWithPackage);
        } catch (ClassNotFoundException ignored) {
            String javaCode = generateClass.apply(this, className);
            try {
                return CACHED_COMPILER.loadFromJava(cl, classNameWithPackage, javaCode);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    }
}