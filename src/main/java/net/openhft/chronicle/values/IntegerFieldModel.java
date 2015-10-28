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
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static net.openhft.chronicle.values.Primitives.widthInBits;
import static net.openhft.chronicle.values.RangeImpl.*;

class IntegerFieldModel extends PrimitiveFieldModel {

    static final Function<String, String> VOLATILE_ACCESS_TYPE = s -> "Volatile" + s;
    static final Function<String, String> ORDERED_ACCESS_TYPE = s -> "Ordered" + s;
    static final Function<String, String> NORMAL_ACCESS_TYPE = identity();

    /**
     * {@code IntegerFieldModel} is used as back-end, so to query bitOffset and extent from {@link
     * ValueModel} it should know the outer model.
     */
    final FieldModel outerModel;

    IntegerFieldModel() {
        outerModel = this;
    }

    IntegerFieldModel(FieldModel outerModel) {
        this.outerModel = outerModel;
    }

    Range range;

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);

        Parameter annotatedParameter = template.annotatedParameter.apply(m);
        if (annotatedParameter == null)
            return;
        Range paramRange = annotatedParameter.getAnnotation(Range.class);
        if (paramRange != null) {
            if (range != null) {
                throw new IllegalStateException("@Range should be specified only once for " + name +
                        " field. Specified " + range + " and " + paramRange);
            }
            long min = paramRange.min();
            long max = paramRange.max();
            if (min >= max) {
                throw new IllegalStateException(paramRange +
                        ": min should be less than max, field " + name);
            }
            if (min < defaultRange().min() || max > defaultRange().max()) {
                throw new IllegalStateException(range + " out of extent of " + type + " type " +
                        "of the field " + name);
            }
            range = paramRange;
        }
    }

    private Range defaultRange() {
        if (type == byte.class) return DEFAULT_BYTE_RANGE;
        if (type == char.class) return DEFAULT_CHAR_RANGE;
        if (type == short.class) return DEFAULT_SHORT_RANGE;
        if (type == int.class) return DEFAULT_INT_RANGE;
        if (type == long.class) return DEFAULT_LONG_RANGE;
        throw new AssertionError("not an integer type: " + type);
    }

    private Range range() {
        return range != null ? range : defaultRange();
    }

    @Override
    int sizeInBits() {
        Range range = range();
        int coverBits;
        long options = range.max() - range.min() + 1;
        if (options <= 0) {
            coverBits = 64;
        } else {
            coverBits = coverBits(options);
        }
        if (coverBits > widthInBits(type))
            throw new IllegalStateException(range + " too wide for " + type + " type");
        return sizeInBitsConsideringVolatileOrOrderedPuts(coverBits);
    }

    private static int coverBits(long options) {
        assert options > 0;
        if (options == 1)
            return 1;
        return Maths.intLog2(options - 1) + 1;
    }

    private static String read(String offset, int bitsToRead, Function<String, String> accessType) {
        return format("bs.read%s(%s)",
                accessType.apply(integerBytesMethodSuffix(bitsToRead)), offset);
    }

    private static String integerBytesMethodSuffix(int bitsToRead) {
        switch (bitsToRead) {
            case 8: return "Byte";
            case 16: return "Short";
            case 32: return "Int";
            case 64: return "Long";
            default: throw new AssertionError("cannot read/write " + bitsToRead + " bits");
        }
    }

    final MemberGenerator nativeGenerator = new IntegerBackedMemberGenerator(this, this) {

        @Override
        protected void finishGet(MethodSpec.Builder methodBuilder, String value) {
            methodBuilder.addStatement("return " + value);
        }

        @Override
        protected String startSet(MethodSpec.Builder methodBuilder) {
            String value = varName(); // parameter name
            Range range = range();
            String checkCondition = checkCondition(value, range);
            if (!checkCondition.isEmpty()) {
                methodBuilder.beginControlFlow(format("if (%s)", checkCondition));
                methodBuilder.addStatement("throw new $T($S + $N + $S)",
                        IllegalArgumentException.class,
                        value + format(" should be in [%d, %d] range, ", range.min(), range.max()),
                        value, " is given");
                methodBuilder.endControlFlow();
            }
            return value;
        }

        @NotNull
        private String checkCondition(String value, Range range) {
            Range defaultRange = defaultRange();
            String cond = "|| ";
            if (range.min() != defaultRange.min())
                cond += value + " < " + range.min();
            if (range.max() != defaultRange.max())
                cond += "|| " + value + " > " + range.max();
            return cond.substring(3);
        }

        @Override
        public void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            // TODO use addAndGetXxxNotAtomic from BytesStore interface when possible
            String value = genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            methodBuilder.addStatement("$T $N = " + value, type, oldName());
            methodBuilder.addStatement("$T $N = $N + $N", type, newName(), oldName(), "addition");
            Range range = range();
            String checkCondition = checkCondition(newName(), range);
            if (!checkCondition.isEmpty()) {
                methodBuilder.beginControlFlow(format("if (%s)", checkCondition));
                methodBuilder.addStatement("throw new $T($S + $N + $S + $N + $S + $N + $S)",
                        IllegalStateException.class,
                        value + format(" should be in [%d, %d] range, the value was ",
                                range.min(), range.max()),
                        oldName(), ", + ", "addition", " = ", newName(), " out of the range");
                methodBuilder.endControlFlow();
            }
            genSet(valueBuilder, methodBuilder, newName(), NORMAL_ACCESS_TYPE);
            methodBuilder.addStatement("return $N", newName());
        }

        @Override
        public void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int bitOffset = valueBuilder.model.fieldBitOffset(outerModel);
            if (bitOffset % 8 == 0) {
                Range range = range();
                int byteOffset = bitOffset / 8;
                if (DEFAULT_INT_RANGE.equals(range) && type == int.class) {
                    methodBuilder.addStatement("return bs.addAndGetInt(offset + $L, addition)",
                            byteOffset);
                } else if (DEFAULT_LONG_RANGE.equals(range)) {
                    methodBuilder.addStatement("return bs.addAndGetLong(offset + $L, addition)",
                            byteOffset);
                } else {
                    throw new UnsupportedOperationException("not implemented yet");
                }
            } else {
                throw new UnsupportedOperationException("not implemented yet");
            }
        }

        @Override
        public void generateCompareAndSwap(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int bitOffset = valueBuilder.model.fieldBitOffset(outerModel);
            if (bitOffset % 8 == 0) {
                Range range = range();
                int byteOffset = bitOffset / 8;
                if (DEFAULT_INT_RANGE.equals(range) && type == int.class) {
                    methodBuilder.addStatement("return bs.compareAndSwapInt(offset + $L, $N, $N)",
                            byteOffset, oldName(), newName());
                } else if (DEFAULT_LONG_RANGE.equals(range)) {
                    methodBuilder.addStatement("return bs.compareAndSwapLong(offset + $L, $N, $N)",
                            byteOffset, oldName(), newName());
                } else {
                    throw new UnsupportedOperationException("not implemented yet");
                }
            } else {
                throw new UnsupportedOperationException("not implemented yet");
            }
        }
    };

    // The methods below are named with "gen" prefix instead of "generate" to avoid confusion
    // and possible bugs when called from MemberGenerator methods, that have the same names

    String genGet(ValueBuilder valueBuilder, Function<String, String> accessType) {
        int bitOffset = valueBuilder.model.fieldBitOffset(outerModel);
        int byteOffset = bitOffset / 8;
        int bitExtent = valueBuilder.model.fieldBitExtent(outerModel);
        String readOffset = "offset + " + byteOffset;
        int lowMaskBits = bitOffset - (byteOffset * 8);
        return genGet(lowMaskBits, bitExtent, readOffset, accessType);
    }

    private String genGet(
            int lowMaskBits, int bitExtent, String readOffset,
            Function<String, String> accessType) {
        int leastBitsToRead = lowMaskBits + sizeInBits();
        int bitsToRead = Maths.nextPower2(leastBitsToRead, 8);
        int highMaskBits = Math.max(bitsToRead - bitExtent, 0);

        String read = read(readOffset, bitsToRead, accessType);
        long readMin = (-1L) << (bitsToRead - 1);
        long readMax = -(readMin + 1);
        Range range = range();

        // No read value translation
        if (readMin <= range.min() && readMax >= range.max())
            return read;

        // "Unsigned" value. This is a special case of the next next block, but treated
        // differently, because `readByte() & 0xFF` looks more familiar than `readByte() + 128`,
        // also the first form is optimized on assembly level by HotSpot (unsigned treatment of
        // the value, no actual `& 0xFF` op), not sure about the second form.
        if (range.min() == 0 && range.max() == readMax - readMin) {
            char[] m = new char[bitsToRead / 4];
            Arrays.fill(m, 'F');
            String mask = "0xFF" + new String(m);
            if (type == long.class)
                mask += "L";
            return format("%s & %s", read, mask);
        }

        // value adjusted to the specified range, no masking
        if (lowMaskBits == 0 && highMaskBits == 0) {
            String add;
            if (type != long.class) {
                assert bitsToRead <= 32 : "long read of int value is possible only if " +
                        "the value spans 5 bytes, therefore must be masked";
                // if add > Integer.MAX_VALUE, it should overflow in int bounds
                add = (((int) range.min()) - ((int) readMin)) + "";
            } else {
                add = (range.min() - readMin) + "L";
            }
            return format("%s + %s", read, add);
        }

        // low or high masks
        assert lowMaskBits > 0 || highMaskBits > 0;
        String masked = read;
        if (lowMaskBits > 0)
            masked = format("%s >> %d", read, lowMaskBits);
        if (highMaskBits > 0) {
            String l = bitsToRead == 64 ? "L" : "";
            masked = format("(%s) & (-1%s >>> %d)", masked, l, highMaskBits);
        }
        if (range.min() != 0) {
            long min = range.min();
            String l = (min < Integer.MAX_VALUE || min > Integer.MAX_VALUE) ? "L" : "";
            masked = format("(%s) + %s", masked, min + l);
        }
        return masked;
    }

    String genArrayElementGet(
            ArrayFieldModel arrayField, ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
            Function<String, String> accessType) {
        int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayField);
        if (arrayBitOffset % 8 != 0)
            throw new UnsupportedOperationException("not implemented yet");
        int arrayByteOffset = arrayBitOffset / 8;
        int elemBitExtent = arrayField.elemBitExtent();
        if (elemBitExtent % 8 == 0) {
            genVerifiedElementOffset(arrayField, methodBuilder);
            String readOffset = format("offset + %d + elementOffset", arrayByteOffset);
            return genGet(0, elemBitExtent, readOffset, accessType);
        } else {
            throw new UnsupportedOperationException("not implemented yet");
        }
    }

    void genSet(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
            String valueToWrite, Function<String, String> accessType) {
        int bitOffset = valueBuilder.model.fieldBitOffset(outerModel);
        int byteOffset = bitOffset / 8;
        String ioOffset = "offset + " + byteOffset;
        int lowMaskBits = bitOffset - (byteOffset * 8);
        int bitExtent = valueBuilder.model.fieldBitExtent(outerModel);
        genSet(methodBuilder, lowMaskBits, bitExtent, ioOffset, accessType, valueToWrite);
    }

    private void genSet(
            MethodSpec.Builder methodBuilder, int lowMaskBits, int bitExtent, String ioOffset,
            Function<String, String> accessType, String valueToWrite) {
        int leastBitsToWrite = lowMaskBits + sizeInBits();
        int bitsToWrite = Maths.nextPower2(leastBitsToWrite, 8);
        int highMaskBits = Math.max(bitsToWrite - bitExtent, 0);

        long writeMin = (-1L) << (bitsToWrite - 1);
        long writeMax = -(writeMin + 1);
        Range range = range();

        if (lowMaskBits > 0 || highMaskBits > 0) {
            assert accessType == NORMAL_ACCESS_TYPE :
                    "volatile/ordered fields shouldn't have masking";
            int fieldBits = bitsToWrite - lowMaskBits - highMaskBits;
            String mask = "0b" + repeat('1', highMaskBits) + repeat('0', fieldBits) +
                    repeat('1', lowMaskBits);
            String read = read(ioOffset, bitsToWrite, NORMAL_ACCESS_TYPE);
            if (lowMaskBits > 0)
                valueToWrite = format("(%s << %s)", valueToWrite, lowMaskBits);
            valueToWrite = format("(%s & %s) | %s", read, mask, valueToWrite);
        }

        String writeMethod = "write" + accessType.apply(integerBytesMethodSuffix(bitsToWrite));
        String write = format("bs.%s(%s, %s)", writeMethod, ioOffset, valueToWrite);
        methodBuilder.addStatement(write);
    }

    void genArrayElementSet(
            ArrayFieldModel arrayField, ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
            Function<String, String> accessType, String valueToWrite) {
        int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayField);
        if (arrayBitOffset % 8 != 0)
            throw new UnsupportedOperationException("not implemented yet");
        int arrayByteOffset = arrayBitOffset / 8;
        int elemBitExtent = arrayField.elemBitExtent();
        if (elemBitExtent % 8 == 0) {
            genVerifiedElementOffset(arrayField, methodBuilder);
            String ioOffset = format("offset + %d + elementOffset", arrayByteOffset);
            genSet(methodBuilder, 0, elemBitExtent, ioOffset, accessType, valueToWrite);
        } else {
            throw new UnsupportedOperationException("not implemented yet");
        }
    }

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new NumberHeapMemberGenerator(this);
    }

    private static String repeat(char c, int n) {
        char[] chars = new char[n];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
