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
