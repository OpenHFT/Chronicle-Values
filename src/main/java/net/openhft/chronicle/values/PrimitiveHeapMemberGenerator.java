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

import static java.lang.String.format;
import static net.openhft.chronicle.values.Primitives.boxed;
import static net.openhft.chronicle.values.Utils.capitalize;

class PrimitiveHeapMemberGenerator extends HeapMemberGenerator {

    final String capType;
    private final String upperType;

    PrimitiveHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
        assert fieldType().isPrimitive();
        capType = capitalize(fieldType().getName());
        upperType = fieldType().getName().toUpperCase();
    }

    @Override
    Class fieldType() {
        Class modelType = super.fieldType();
        if (modelType == long.class || modelType == int.class)
            return modelType;
        PrimitiveFieldModel fieldModel = (PrimitiveFieldModel) this.fieldModel;
        if (fieldModel.setOrdered != null || fieldModel.compareAndSwap != null ||
                fieldModel.addAtomic != null) {
            if (modelType == float.class || modelType == double.class)
                throw new UnsupportedOperationException();
            return int.class;
        }
        return modelType;
    }

    private String getVolatile() {
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
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        if (fieldType() != fieldModel.type) {
            methodBuilder.addStatement("return ($T) $N", fieldModel.type, field);
        } else {
            super.generateGet(valueBuilder, methodBuilder);
        }
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        if (fieldType() != fieldModel.type) {
            methodBuilder.addStatement("return ($T) $N[index]", fieldModel.type, field);
        } else {
            super.generateArrayElementGet(arrayFieldModel, valueBuilder, methodBuilder);
        }
    }

    @Override
    public void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(format("return %s$N.$N(this, $N)", getCast()),
                valueBuilder.unsafe(), getVolatile(), fieldOffset(valueBuilder));
    }

    @Override
    public void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement(
                format("return %s$N.$N($N, (long) $T.$N + (index * (long) $T.$N))", getCast()),
                valueBuilder.unsafe(), getVolatile(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale());
    }

    String getCast() {
        return fieldType() == fieldModel.type ? "" :
                format("(%s) ", fieldModel.type.getName());
    }

    @Override
    void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String cast = fieldModel.type == char.class ? "(char) " : "";
        methodBuilder.addStatement("$N = $Nbytes.$N()",
                fieldModel.fieldName(), cast, fieldModel.readMethod());
    }

    @Override
    void generateArrayElementReadMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String cast = fieldModel.type == char.class ? "(char) " : "";
        methodBuilder.addStatement("$N[index] = $Nbytes.$N()",
                fieldModel.fieldName(), cast, fieldModel.readMethod());
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
