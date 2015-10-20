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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static net.openhft.chronicle.values.Primitives.widthInBits;
import static net.openhft.chronicle.values.RangeImpl.*;

public class IntegerFieldModel extends PrimitiveFieldModel {

    private static Function<String, String> VOLATILE_ACCESS_TYPE = s -> "Volatile" + s;
    private static Function<String, String> ORDERED_ACCESS_TYPE = s -> s + "Ordered";
    private static Function<String, String> NORMAL_ACCESS_TYPE = identity();

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

    private static String read(int byteOffset, int bitsToRead,
                               Function<String, String> accessType) {
        return format("bs.read%s(offset + %d)",
                accessType.apply(integerBytesMethodSuffix(bitsToRead)), byteOffset);
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

    private final ValueMemberGenerator nativeGenerator = new ValueMemberGenerator() {
        @Override
        public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("return " + genNormalGet(valueBuilder));
        }

        @Override
        public void generateGetVolatile(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("return " + genVolatileGet(valueBuilder));
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String valueToWrite = name; // parameter name
            genNormalSet(valueBuilder, methodBuilder, valueToWrite);
        }

        @Override
        public void generateSetVolatile(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String valueToWrite = name; // parameter name
            genVolatileSet(valueBuilder, methodBuilder, valueToWrite);
        }

        @Override
        public void generateSetOrdered(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String valueToWrite = name; // parameter name
            genSetOrdered(valueBuilder, methodBuilder, valueToWrite);
        }
    };

    // The methods below are named with "gen" prefix instead of "generate" to avoid confusion
    // and possible bugs when called from ValueMemberGenerator methods, that have the same names

    String genNormalGet(ValueBuilder valueBuilder) {
        return genGet(valueBuilder, NORMAL_ACCESS_TYPE);
    }

    String genVolatileGet(ValueBuilder valueBuilder) {
        return genGet(valueBuilder, VOLATILE_ACCESS_TYPE);
    }

    private String genGet(ValueBuilder valueBuilder, Function<String, String> accessType) {
        int bitOffset = valueBuilder.model.fieldBitOffset(IntegerFieldModel.this);
        int byteOffset = bitOffset / 8;
        int lowMaskBits = bitOffset - (byteOffset * 8);
        int leastBitsToRead = lowMaskBits + sizeInBits();
        int bitsToRead = Maths.nextPower2(leastBitsToRead, 8);
        int bitExtent = valueBuilder.model.fieldBitExtent(IntegerFieldModel.this);
        int highMaskBits = Math.max(bitsToRead - bitExtent, 0);

        String read = read(byteOffset, bitsToRead, accessType);
        long readMin = (-1L) << (bitsToRead - 1);
        long readMax = -(readMin + 1);
        Range range = range();

        // No read value translation
        if (readMin == range.min() && readMax == range.max())
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

    void genNormalSet(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String valueToWrite) {
        genSet(valueBuilder, methodBuilder, valueToWrite, NORMAL_ACCESS_TYPE);
    }

    void genVolatileSet(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String valueToWrite) {
        genSet(valueBuilder, methodBuilder, valueToWrite, VOLATILE_ACCESS_TYPE);
    }

    void genSetOrdered(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String valueToWrite) {
        genSet(valueBuilder, methodBuilder, valueToWrite, ORDERED_ACCESS_TYPE);
    }

    private void genSet(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
            String valueToWrite, Function<String, String> accessType) {
        int bitOffset = valueBuilder.model.fieldBitOffset(IntegerFieldModel.this);
        int byteOffset = bitOffset / 8;
        int lowMaskBits = bitOffset - (byteOffset * 8);
        int leastBitsToWrite = lowMaskBits + sizeInBits();
        int bitsToWrite = Maths.nextPower2(leastBitsToWrite, 8);
        int bitExtent = valueBuilder.model.fieldBitExtent(IntegerFieldModel.this);
        int highMaskBits = Math.max(bitsToWrite - bitExtent, 0);

        long writeMin = (-1L) << (bitsToWrite - 1);
        long writeMax = -(writeMin + 1);
        Range range = range();


        if (writeMin != range.min() || writeMax != range.max()) {
            assert accessType == NORMAL_ACCESS_TYPE :
                    "volatile/ordered fields shouldn't have masking";
            int fieldBits = bitsToWrite - lowMaskBits - highMaskBits;
            String mask = "0b" + repeat('1', highMaskBits) + repeat('0', fieldBits) +
                    repeat('1', lowMaskBits);
            String read = read(byteOffset, bitsToWrite, NORMAL_ACCESS_TYPE);
            valueToWrite = format(
                    "(%s & %s) | (%s << %s)", read, mask, valueToWrite, lowMaskBits);
        }

        String writeMethod = "write" + accessType.apply(integerBytesMethodSuffix(bitsToWrite));
        String write = format("bs.%s(offset + %d, %s)", writeMethod, byteOffset, valueToWrite);
        methodBuilder.addStatement(write);
    }

    @Override
    ValueMemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    private String repeat(char c, int n) {
        char[] chars = new char[n];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
