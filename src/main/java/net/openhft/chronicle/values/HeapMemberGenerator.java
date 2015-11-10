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

import static java.lang.String.format;
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
                    .build();
            valueBuilder.staticBlockBuilder().addStatement(
                    "$N = $N.objectFieldOffset($T.getField($N.class, $S))",
                    fieldOffset, valueBuilder.unsafe(), Jvm.class, valueBuilder.className,
                    field.name);
            valueBuilder.typeBuilder.addField(fieldOffset);
        }
        return fieldOffset;
    }

    Class fieldType() {
        return fieldModel.type;
    }

    abstract String wrap(String rawStoredValue);

    abstract String unwrap(String inputValue);

    @Override
    void generateFields(ValueBuilder valueBuilder) {
        field = FieldSpec.builder(fieldType(), fieldModel.fieldName(), PRIVATE).build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    void generateArrayElementFields(ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
        field = FieldSpec.builder(ArrayTypeName.of(fieldType()), fieldModel.fieldName())
                .addModifiers(PRIVATE)
                .initializer("new $T[$L]", fieldType(), arrayFieldModel.array.length())
                .build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return " + wrap("$N"), field);
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return " + wrap("$N[index]"), field);
    }

    @Override
    public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N = " + unwrap("$N"), field, fieldModel.varName());
    }

    @Override
    public void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N[index] = " + unwrap("$N"), field, fieldModel.varName());
    }

    @Override
    public void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(format("$N.$N(this, $N, %s)", unwrap("$N")),
                valueBuilder.unsafe(), putVolatile(), fieldOffset(valueBuilder),
                fieldModel.varName());
    }

    @Override
    public void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement(
                format("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s)", unwrap("$N")),
                valueBuilder.unsafe(), putVolatile(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale(), fieldModel.varName());
    }

    @Override
    public void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(format("$N.$N(this, $N, %s)", unwrap("$N")),
                valueBuilder.unsafe(), putOrdered(), fieldOffset(valueBuilder),
                fieldModel.varName());
    }

    @Override
    public void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement(
                format("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s)", unwrap("$N")),
                valueBuilder.unsafe(), putOrdered(), field, Unsafe.class, arrayBase(), Unsafe.class,
                arrayScale(), fieldModel.varName());
    }

    @Override
    public void generateCompareAndSwap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(
                format("return $N.$N(this, $N, %s, %s)", unwrap("$N"), unwrap("$N")),
                valueBuilder.unsafe(), compareAndSwap(), fieldOffset(valueBuilder),
                fieldModel.oldName(), fieldModel.newName());
    }

    @Override
    public void generateArrayElementCompareAndSwap(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement(
                format("return $N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s, %s)",
                        unwrap("$N"), unwrap("$N")),
                valueBuilder.unsafe(), compareAndSwap(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale(), fieldModel.oldName(), fieldModel.newName());
    }

    @Override
    public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(format("this.$N = %s", unwrap("from.$N()")),
                field, fieldModel.getOrGetVolatile().getName());
    }

    @Override
    public void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(format("this.$N[index] = %s", unwrap("from.$N(index)")),
                field, arrayFieldModel.getOrGetVolatile().getName());
    }

    @Override
    void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        genToString(methodBuilder, wrap(field.name));
    }

    @Override
    void generateArrayElementToString(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        genArrayElementToString(methodBuilder, wrap(field.name + "[index]"));
    }
}
