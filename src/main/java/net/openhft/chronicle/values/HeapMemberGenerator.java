/*
 *      Copyright (C) 2015, 2016-2020 chronicle.software
 *      Copyright (C) 2016 Roman Leventov
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
import static net.openhft.chronicle.values.Utils.capitalize;

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

    abstract String wrap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String rawStoredValue);

    abstract String unwrap(MethodSpec.Builder methodBuilder, String inputValue);

    @Override
    void generateFields(ValueBuilder valueBuilder) {
        field = FieldSpec.builder(fieldType(), fieldModel.fieldName(), PRIVATE).build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    void generateArrayElementFields(ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
        field = FieldSpec.builder(ArrayTypeName.of(fieldType()), fieldModel.fieldName())
                .addModifiers(PRIVATE, FINAL)
                .initializer("new $T[$L]", fieldType(), arrayFieldModel.array.length())
                .build();
        valueBuilder.typeBuilder.addField(field);
    }

    @Override
    public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N", fieldType(), rawValue, field);
        methodBuilder.addStatement("return $N", wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    public void generateArrayElementGet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N[index]", fieldType(), rawValue, field);
        methodBuilder.addStatement("return $N", wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N = $N",
                field, unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateArrayElementSet(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("this.$N[index] = $N",
                field, unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$N.$N(this, $N, $N)",
                valueBuilder.unsafe(), putVolatile(), fieldOffset(valueBuilder),
                unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateArrayElementSetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement(
                format("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s)",
                        unwrap(methodBuilder, fieldModel.varName())),
                valueBuilder.unsafe(), putVolatile(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale());
    }

    @Override
    public void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$N.$N(this, $N, $N)",
                valueBuilder.unsafe(), putOrdered(), fieldOffset(valueBuilder),
                unwrap(methodBuilder, fieldModel.varName()));
    }

    @Override
    public void generateArrayElementSetOrdered(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement(
                format("$N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s)",
                        unwrap(methodBuilder, fieldModel.varName())),
                valueBuilder.unsafe(), putOrdered(), field, Unsafe.class, arrayBase(), Unsafe.class,
                arrayScale());
    }

    @Override
    public void generateCompareAndSwap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String unwrappedOld = unwrap(methodBuilder, fieldModel.oldName());
        String unwrappedNew = unwrap(methodBuilder, fieldModel.newName());
        methodBuilder.addStatement("return $N.$N(this, $N, $N, $N)",
                valueBuilder.unsafe(), compareAndSwap(), fieldOffset(valueBuilder),
                unwrappedOld, unwrappedNew);
    }

    @Override
    public void generateArrayElementCompareAndSwap(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        String unwrappedOld = unwrap(methodBuilder, fieldModel.oldName());
        String unwrappedNew = unwrap(methodBuilder, fieldModel.newName());
        methodBuilder.addStatement(
                "return $N.$N($N, (long) $T.$N + (index * (long) $T.$N), $N, $N)",
                valueBuilder.unsafe(), compareAndSwap(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale(), unwrappedOld, unwrappedNew);
    }

    @Override
    public void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String copy = fieldModel.name + "Copy";
        methodBuilder.addStatement("$T $N = from.$N()",
                fieldModel.type, copy, fieldModel.getOrGetVolatile().getName());
        methodBuilder.addStatement("this.$N = $N", field, unwrap(methodBuilder, copy));
    }

    @Override
    public void generateArrayElementCopyFrom(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String copy = arrayFieldModel.name + "Copy";
        methodBuilder.addStatement("$T $N = from.$N(index)",
                arrayFieldModel.type, copy, arrayFieldModel.getOrGetVolatile().getName());
        methodBuilder.addStatement("this.$N[index] = $N", field, unwrap(methodBuilder, copy));
    }

    @Override
    void generateToString(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N", fieldType(), rawValue, field);
        genToString(methodBuilder, wrap(valueBuilder, methodBuilder, rawValue));
    }

    @Override
    void generateArrayElementToString(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        String rawValue = "raw" + capitalize(field.name) + "Value";
        methodBuilder.addStatement("$T $N = $N[index]", fieldType(), rawValue, field);
        genArrayElementToString(methodBuilder, wrap(valueBuilder, methodBuilder, rawValue));
    }
}
