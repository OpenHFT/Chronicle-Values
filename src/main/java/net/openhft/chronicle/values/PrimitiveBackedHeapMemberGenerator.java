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
import sun.misc.Unsafe;

import static net.openhft.chronicle.values.Primitives.boxed;
import static net.openhft.chronicle.values.Utils.capitalize;

class PrimitiveBackedHeapMemberGenerator extends HeapMemberGenerator {

    final String capType;
    private final String upperType;
    private final Class fieldType;

    PrimitiveBackedHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
        fieldType = determineFieldType();
        assert fieldType.isPrimitive();
        capType = capitalize(fieldType.getName());
        upperType = fieldType.getName().toUpperCase();
    }

    PrimitiveBackedHeapMemberGenerator(FieldModel fieldModel, Class fieldType) {
        super(fieldModel);
        this.fieldType = fieldType;
        assert fieldType.isPrimitive();
        capType = capitalize(fieldType.getName());
        upperType = fieldType.getName().toUpperCase();
    }

    @Override
    Class fieldType() {
        return fieldType;
    }

    private Class determineFieldType() {
        Class modelType = super.fieldType();
        if (modelType == long.class || modelType == int.class)
            return modelType;
        PrimitiveFieldModel fieldModel = (PrimitiveFieldModel) this.fieldModel;
        if (fieldModel.setOrdered != null || fieldModel.compareAndSwap != null ||
                fieldModel.addAtomic != null) {
            if (modelType == double.class)
                return long.class;
            return int.class;
        }
        return modelType;
    }

    String getVolatile() {
        return "get" + capType + "Volatile";
    }

    @Override
    String putVolatile() {
        return "put" + capType + "Volatile";
    }

    @Override
    String putOrdered() {
        return "putOrdered" + capType;
    }

    @Override
    String compareAndSwap() {
        return "compareAndSwap" + capType;
    }

    @Override
    String arrayBase() {
        return "ARRAY_" + upperType + "_BASE_OFFSET";
    }

    @Override
    String arrayScale() {
        return "ARRAY_" + upperType + "_INDEX_SCALE";
    }

    @Override
    String wrap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String rawStoredValue) {
        if (fieldType() == fieldModel.type)
            return rawStoredValue;
        if (fieldModel.type == float.class)
            return "java.lang.Float.intBitsToFloat(" + rawStoredValue + ")";
        if (fieldModel.type == double.class)
            return "java.lang.Double.longBitsToDouble(" + rawStoredValue + ")";
        if (fieldModel.type == boolean.class)
            return "(" + rawStoredValue + " != 0)";
        assert fieldModel.type == byte.class || fieldModel.type == short.class ||
                fieldModel.type == char.class;
        return "((" + fieldModel.type.getSimpleName() + ") " + rawStoredValue + ")";
    }

    @Override
    String unwrap(MethodSpec.Builder methodBuilder, String inputValue) {
        if (fieldType() == fieldModel.type)
            return inputValue;
        if (fieldModel.type == float.class)
            return "java.lang.Float.floatToRawIntBits(" + inputValue + ")";
        if (fieldModel.type == double.class)
            return "java.lang.Double.doubleToRawLongBits(" + inputValue + ")";
        if (fieldModel.type == boolean.class)
            return "(" + inputValue + " ? 1 : 0)";
        assert fieldModel.type == byte.class || fieldModel.type == short.class ||
                fieldModel.type == char.class;
        return inputValue; // byte, short, char -- auto widening, no explicit conversion needed
    }

    @Override
    public void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N.$N(this, $N)",
                fieldType(), rawValue,
                valueBuilder.unsafe(), getVolatile(), fieldOffset(valueBuilder));
        methodBuilder.addStatement("return $N", wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    public void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N.$N($N, (long) $T.$N + (index * (long) $T.$N))",
                fieldType(), rawValue,
                valueBuilder.unsafe(), getVolatile(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale());
        methodBuilder.addStatement("return $N", wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        if (fieldModel.type != char.class || fieldType() != fieldModel.type) {
            methodBuilder.addStatement("$N = " + unwrap(methodBuilder, "bytes.$N()"),
                    field, fieldModel.readMethod());
        } else {
            methodBuilder.addStatement("$N = (char) bytes.readUnsignedShort()", field);
        }
    }

    @Override
    void generateArrayElementReadMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        if (fieldModel.type != char.class || fieldType() != fieldModel.type) {
            methodBuilder.addStatement("$N[index] = " + unwrap(methodBuilder, "bytes.$N()"),
                    field, fieldModel.readMethod());
        } else {
            methodBuilder.addStatement("$N[index] = (char) bytes.readUnsignedShort()", field);
        }
    }

    @Override
    void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addCode("if ($N() != other.$N()) return false;\n",
                fieldModel.getOrGetVolatile().getName(), fieldModel.getOrGetVolatile().getName());
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
        return String.format("%s.hashCode(%s())",
                boxed(fieldModel.type).getName(), fieldModel.getOrGetVolatile().getName());
    }

    @Override
    String generateArrayElementHashCode(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        return String.format("%s.hashCode(%s(index))",
                boxed(fieldModel.type).getName(), arrayFieldModel.getOrGetVolatile().getName());
    }
}
