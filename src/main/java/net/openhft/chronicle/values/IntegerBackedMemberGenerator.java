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

import static java.lang.String.format;
import static net.openhft.chronicle.values.IntegerFieldModel.*;
import static net.openhft.chronicle.values.Primitives.boxed;

abstract class IntegerBackedMemberGenerator extends MemberGenerator {

    final IntegerFieldModel backingFieldModel;

    protected IntegerBackedMemberGenerator(
            FieldModel fieldModel, IntegerFieldModel backingFieldModel) {
        super(fieldModel);
        this.backingFieldModel = backingFieldModel;
    }

    protected abstract void finishGet(MethodSpec.Builder methodBuilder, String value);

    /**
     * Returns integer value to write (as string)
     */
    protected abstract String startSet(MethodSpec.Builder methodBuilder);

    @Override
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        finishGet(methodBuilder, backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
        finishGet(methodBuilder, value);
    }

    @Override
    public void generateGetVolatile(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        finishGet(methodBuilder, backingFieldModel.genGet(valueBuilder, VOLATILE_ACCESS_TYPE));
    }

    @Override
    public void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, VOLATILE_ACCESS_TYPE);
        finishGet(methodBuilder, value);
    }

    @Override
    public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, valueToWrite, NORMAL_ACCESS_TYPE);
    }

    @Override
    public void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                NORMAL_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateSetVolatile(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, valueToWrite, VOLATILE_ACCESS_TYPE);
    }

    @Override
    public void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                VOLATILE_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateSetOrdered(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, valueToWrite, ORDERED_ACCESS_TYPE);
    }

    @Override
    public void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                ORDERED_ACCESS_TYPE, valueToWrite);
    }

    @Override
    void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        if (fieldModel.set != null) {
            methodBuilder.addStatement("$N(from.$N())",
                    fieldModel.set.getName(), fieldModel.getOrGetVolatile().getName());
        } else {
            methodBuilder.addStatement("$T $N = from.$N()",
                    fieldModel.type, fieldModel.varName(), fieldModel.getOrGetVolatile().getName());
            generateSet(valueBuilder, methodBuilder);
        }
    }

    @Override
    void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        if (arrayFieldModel.set != null) {
            methodBuilder.addStatement("$N(index, from.$N(index))",
                    arrayFieldModel.set.getName(), arrayFieldModel.getOrGetVolatile().getName());
        } else {
            methodBuilder.addStatement("$T $N = from.$N(index)",
                    fieldModel.type, fieldModel.varName(),
                    arrayFieldModel.getOrGetVolatile().getName());
            generateArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder);
        }
    }

    @Override
    void generateWriteMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("bytes.write$N($N)", backingFieldModel.capTypeName(),
                backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
    }

    @Override
    void generateArrayElementWriteMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
        methodBuilder.addStatement("bytes.write$N($N)", backingFieldModel.capTypeName(), value);
    }

    @Override
    void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        backingFieldModel.genSet(valueBuilder, methodBuilder, readValue(), NORMAL_ACCESS_TYPE);
    }

    @Override
    void generateArrayElementReadMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                NORMAL_ACCESS_TYPE, readValue());
    }

    private String readValue() {
        return format("bytes.read%s()", backingFieldModel.capTypeName());
    }
}
