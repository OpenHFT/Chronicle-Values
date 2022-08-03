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

import com.squareup.javapoet.MethodSpec;

import static java.lang.String.format;
import static net.openhft.chronicle.values.IntegerFieldModel.*;

abstract class IntegerBackedNativeMemberGenerator extends MemberGenerator {

    final IntegerFieldModel backingFieldModel;

    IntegerBackedNativeMemberGenerator(
            FieldModel fieldModel, IntegerFieldModel backingFieldModel) {
        super(fieldModel);
        this.backingFieldModel = backingFieldModel;
    }

    abstract void finishGet(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String value);

    /**
     * Returns integer value to write (as string)
     */
    abstract String startSet(MethodSpec.Builder methodBuilder);

    @Override
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateGetVolatile(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genGet(valueBuilder, VOLATILE_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, VOLATILE_ACCESS_TYPE);
        finishGet(valueBuilder, methodBuilder, value);
    }

    @Override
    public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                NORMAL_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateSetVolatile(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, VOLATILE_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                VOLATILE_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateSetOrdered(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String valueToWrite = startSet(methodBuilder);
        backingFieldModel.genSet(valueBuilder, methodBuilder, ORDERED_ACCESS_TYPE, valueToWrite);
    }

    @Override
    public void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
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
        methodBuilder.addStatement("bytes.$N($N)", backingFieldModel.writeMethod(),
                backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
    }

    @Override
    void generateArrayElementWriteMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String value = backingFieldModel.genArrayElementGet(
                arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
        methodBuilder.addStatement("bytes.$N($N)", backingFieldModel.writeMethod(), value);
    }

    @Override
    void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        backingFieldModel.genSet(valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE, readValue());
    }

    @Override
    void generateArrayElementReadMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        backingFieldModel.genArrayElementSet(arrayFieldModel, valueBuilder, methodBuilder,
                NORMAL_ACCESS_TYPE, readValue());
    }

    private String readValue() {
        return format("bytes.%s()", backingFieldModel.readMethod());
    }
}
