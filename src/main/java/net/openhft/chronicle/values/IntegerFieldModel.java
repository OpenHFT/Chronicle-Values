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
import static net.openhft.chronicle.values.Primitives.boxed;
import static net.openhft.chronicle.values.Primitives.widthInBits;
import static net.openhft.chronicle.values.RangeImpl.*;
import static net.openhft.chronicle.values.Utils.capitalize;
import static net.openhft.chronicle.values.Utils.formatIntOrLong;

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
    void addTypeInfo(Method m, MethodTemplate template) {
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
        return capitalize(integerBytesIoType(bitsToRead).getSimpleName());
    }

    private static Class integerBytesIoType(int bits) {
        switch (bits) {
            case 8: return byte.class;
            case 16: return short.class;
            case 32: return int.class;
            case 64: return long.class;
            default: throw new AssertionError("cannot read/write " + bits + " bits");
        }
    }

    final MemberGenerator nativeGenerator = new IntegerBackedMemberGenerator(this, this) {

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            // no fields
        }

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
            String cond = " || ";
            if (range.min() != defaultRange.min())
                cond += value + " < " + formatIntOrLong(range.min());
            if (range.max() != defaultRange.max())
                cond += " || " + value + " > " + formatIntOrLong(range.max());
            return cond.substring(4);
        }

        @Override
        public void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            // TODO use addAndGetXxxNotAtomic from BytesStore interface when possible
            String value = genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            methodBuilder.addStatement("$T $N = " + value, type, oldName());
            if (type != byte.class && type != short.class && type != char.class) {
                methodBuilder.addStatement("$T $N = $N + $N",
                        type, newName(), oldName(), "addition");
            } else {
                methodBuilder.addStatement("$T $N = ($T) ($N + $N)",
                        type, newName(), type, oldName(), "addition");

            }
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
            return format("%s.hashCode(%s())", boxed(type).getName(), getOrGetVolatile().getName());
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            return format("%s.hashCode(%s(index))",
                    boxed(type).getName(), arrayFieldModel.getOrGetVolatile().getName());
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
        int highMaskBits = Math.max(bitsToRead - bitExtent - lowMaskBits, 0);
        int fieldBits = bitsToRead - lowMaskBits - highMaskBits;

        String read = read(readOffset, bitsToRead, accessType);

        long readMin = (-1L) << (fieldBits - 1);
        long readMax = -(readMin + 1);

        if (lowMaskBits > 0 || highMaskBits > 0) {
            if (lowMaskBits > 0)
                read = format("%s >> %d", read, lowMaskBits);
            if (highMaskBits > 0) {
                String l = bitsToRead == 64 ? "L" : "";
                read = format("(%s) & (-1%s >>> %d)", read, l, highMaskBits);
                readMin = 0;
                readMax = (1L << fieldBits) - 1;
            }
        }

        Range range = range();

        // No read value translation
        if (readMin <= range.min() && readMax >= range.max())
            return read;

        // "Unsigned" value. This is a special case of the next next block, but treated
        // differently, because `readByte() & 0xFF` looks more familiar than `readByte() + 128`,
        // also the first form is optimized on assembly level by HotSpot (unsigned treatment of
        // the value, no actual `& 0xFF` op), not sure about the second form.
        long readRange = readMax - readMin;
        if (range.min() == 0 &&
                (readRange < 0 || range.max() <= readRange)) { // overflow-aware
            String mask;
            // mask are equivalent, the first representation is more readable
            if (fieldBits % 4 == 0) {
                mask = "0x" + repeat('F', fieldBits / 4);
            } else {
                mask = "0b" + repeat('1', fieldBits);
            }
            if (type == long.class)
                mask += "L";
            return cast(format("%s & %s", read, mask));
        }

        String add;
        if (type != long.class) {
            assert bitsToRead <= 32 : "long read of int value is possible only if " +
                    "the value spans 5 bytes, therefore must be masked";
            // if add > Integer.MAX_VALUE, it should overflow in int bounds
            add = (((int) range.min()) - ((int) readMin)) + "";
        } else {
            add = (range.min() - readMin) + "L";
        }
        return cast(format("%s + %s", read, add));
    }

    private String cast(String value) {
        if (type == byte.class || type == char.class || type == short.class)
            value = format("((%s) (%s))", type.getSimpleName(), value);
        return value;
    }

    String genArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder, Function<String, String> accessType) {
        int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayFieldModel);
        if (arrayBitOffset % 8 != 0)
            throw new UnsupportedOperationException("not implemented yet");
        int arrayByteOffset = arrayBitOffset / 8;
        int elemBitExtent = arrayFieldModel.elemBitExtent();
        if (elemBitExtent % 8 == 0) {
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
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
        int highMaskBits = Math.max(bitsToWrite - bitExtent - lowMaskBits, 0);

        int fieldBits = bitsToWrite - lowMaskBits - highMaskBits;
        long readMin;
        long readMax;
        if (highMaskBits == 0) {
            readMin = (-1L) << (fieldBits - 1);
            readMax = -(readMin + 1);
        } else {
            readMin = 0;
            readMax = (1L << fieldBits) - 1;
        }
        long readRange = readMax - readMin;
        Range range = range();
        if ((readMin > range.min() || readMax < range.max()) &&
                (range.min() != 0 || (readRange >= 0 && range.max() > readRange))) {
            String sub;
            if (type != long.class) {
                assert bitsToWrite <= 32 : "long read of int value is possible only if " +
                        "the value spans 5 bytes, therefore must be masked";
                // if sub > Integer.MAX_VALUE, it should overflow in int bounds
                sub = (((int) range.min()) - ((int) readMin)) + "";
            } else {
                sub = (range.min() - readMin) + "L";
            }
            valueToWrite = format("(%s - %s)", valueToWrite, sub);
        }

        if (lowMaskBits > 0 || highMaskBits > 0) {
            assert accessType == NORMAL_ACCESS_TYPE :
                    "volatile/ordered fields shouldn't have masking";
            String mask;
            if (lowMaskBits % 4 == 0 && fieldBits % 4 == 0 && highMaskBits % 4 == 0) {
                mask = "0x" + repeat('F', highMaskBits / 4) + repeat('0', fieldBits / 4) +
                        repeat('F', lowMaskBits / 4);
            } else {
                mask = "0b" + repeat('1', highMaskBits) + repeat('0', fieldBits) +
                        repeat('1', lowMaskBits);
            }
            if (bitsToWrite == 64)
                mask += "L";
            String read = read(ioOffset, bitsToWrite, NORMAL_ACCESS_TYPE);
            if (lowMaskBits > 0)
                valueToWrite = format("(%s << %s)", valueToWrite, lowMaskBits);
            valueToWrite = format("(%s & %s) | %s", read, mask, valueToWrite);
        }

        Class ioType = integerBytesIoType(bitsToWrite);
        if (ioType != type)
            valueToWrite = format("(%s) (%s)", ioType.getSimpleName(), valueToWrite);
        String writeMethod = "write" + accessType.apply(
                type != char.class ? integerBytesMethodSuffix(bitsToWrite) : "UnsignedShort");
        String write = format("bs.%s(%s, %s)", writeMethod, ioOffset, valueToWrite);
        methodBuilder.addStatement(write);
    }

    void genArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder, Function<String, String> accessType,
            String valueToWrite) {
        int arrayBitOffset = valueBuilder.model.fieldBitOffset(arrayFieldModel);
        if (arrayBitOffset % 8 != 0)
            throw new UnsupportedOperationException("not implemented yet");
        int arrayByteOffset = arrayBitOffset / 8;
        int elemBitExtent = arrayFieldModel.elemBitExtent();
        if (elemBitExtent % 8 == 0) {
            genVerifiedElementOffset(arrayFieldModel, methodBuilder);
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
