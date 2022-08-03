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

abstract class MemberGenerator {

    final FieldModel fieldModel;

    protected MemberGenerator(FieldModel fieldModel) {
        this.fieldModel = fieldModel;
    }

    private Class<? extends FieldModel> fieldModelClass() {
        return fieldModel.getClass();
    }

    void generateFields(ValueBuilder valueBuilder) {
        // do nothing by default
    }

    void generateArrayElementFields(ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementGetUsing(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementAdd(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementAddAtomic(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateCompareAndSwap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementCompareAndSwap(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateWriteMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("bytes.$N($N())",
                fieldModel.writeMethod(), fieldModel.getOrGetVolatile().getName());
    }

    void generateArrayElementWriteMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("bytes.$N($N(index))",
                fieldModel.writeMethod(), arrayFieldModel.getOrGetVolatile().getName());
    }

    void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementReadMarshallable(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateArrayElementEquals(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    String generateArrayElementHashCode(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException(fieldModelClass() + "");
    }

    void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        genToString(methodBuilder, fieldModel.getOrGetVolatile().getName() + "()");
    }

    final void genToString(MethodSpec.Builder methodBuilder, String value) {
        methodBuilder.addStatement("sb.append($S).append($N)",
                ", " + fieldModel.name + "=", value);
    }

    void generateArrayElementToString(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        genArrayElementToString(methodBuilder,
                arrayFieldModel.getOrGetVolatile().getName() + "(index)");
    }

    final void genArrayElementToString(MethodSpec.Builder methodBuilder, String value) {
        methodBuilder.addStatement("sb.append($N).append(',').append(' ')", value);
    }
}
