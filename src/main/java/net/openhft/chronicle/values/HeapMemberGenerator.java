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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import net.openhft.chronicle.core.Jvm;
import sun.misc.Unsafe;

import java.lang.reflect.Method;

import static javax.lang.model.element.Modifier.*;

abstract class HeapMemberGenerator extends MemberGenerator {

    FieldSpec field;
    private FieldSpec fieldOffset;

    HeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    abstract String putVolatile();

    abstract String putOrdered();

    abstract String compareAndSwap();

    abstract String arrayBase();

    abstract String arrayScale();

    FieldSpec fieldOffset(ValueBuilder valueBuilder) {
        if (fieldOffset == null) {
            fieldOffset = FieldSpec.builder(long.class, fieldModel.name + "Offset")
                    .addModifiers(PRIVATE, STATIC, FINAL)
                    .initializer("$N.objectFieldOffset($T.getField($T.class, $S))",
                            valueBuilder.unsafe(), Jvm.class, valueBuilder.className,
                            field.name)
                    .build();
            valueBuilder.typeBuilder.addField(fieldOffset);
        }
        return fieldOffset;
    }

    Class fieldType() {
        return fieldModel.type;
    }

    @Override
    public void generateFields(ValueBuilder valueBuilder) {
        field = FieldSpec.builder(fieldType(), fieldModel.varName(), PRIVATE).build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    public void generateArrayElementFields(ValueBuilder valueBuilder) {
        field = FieldSpec.builder(ArrayTypeName.of(fieldType()), fieldModel.varName(), PRIVATE)
                .build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return $N", field);
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return $N[index]", field);
    }

    @Override
    public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N = $N", field, fieldModel.varName());
    }

    @Override
    public void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N[index] = $N", field, fieldModel.varName());
    }

    @Override
    public void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$N.$N(this, $N, $N)",
                valueBuilder.unsafe(), putVolatile(), fieldOffset(valueBuilder),
                fieldModel.varName());
    }

    @Override
    public void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), $N)",
                valueBuilder.unsafe(), putVolatile(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale(), fieldModel.varName());
    }

    @Override
    public void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$N.$N(this, $N, $N)",
                valueBuilder.unsafe(), putOrdered(), fieldOffset(valueBuilder),
                fieldModel.varName());
    }

    @Override
    public void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), $N)",
                valueBuilder.unsafe(), putOrdered(), field, Unsafe.class, arrayBase(), Unsafe.class,
                arrayScale(), fieldModel.varName());
    }

    @Override
    public void generateCompareAndSwap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$N.$N(this, $N, $N, $N)",
                valueBuilder.unsafe(), compareAndSwap(), fieldOffset(valueBuilder),
                fieldModel.oldName(), fieldModel.newName());
    }

    @Override
    public void generateArrayElementCompareAndSwap(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), $N, $N)",
                valueBuilder.unsafe(), compareAndSwap(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale(), fieldModel.oldName(), fieldModel.newName());
    }

    @Override
    public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N = from.$N()",
                field, fieldModel.getOrGetVolatile().getName());
    }

    @Override
    public void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N[index] = from.$N(index)",
                field, fieldModel.getOrGetVolatile().getName());
    }
}
