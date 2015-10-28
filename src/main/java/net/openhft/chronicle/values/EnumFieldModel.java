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

import java.lang.reflect.Method;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.*;
import static net.openhft.chronicle.values.Nullability.NULLABLE;

class EnumFieldModel extends IntegerBackedFieldModel {

    final FieldNullability nullability = new FieldNullability(this);

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        nullability.addTemplate(m, template);
    }

    @Override
    void postProcess() {
        super.postProcess();
        int min = nullable() ? -1 : 0;
        int constants = Enums.numberOfConstants(type);
        if (constants == 0) {
            throw new IllegalStateException(
                    name + "field type is a enum with zero constants: " + type);
        }
        backend.type = int.class;
        backend.range = new RangeImpl(min, constants - 1);
    }

    private boolean nullable() {
        return nullability.nullability() == NULLABLE;
    }

    final MemberGenerator nativeGenerator = new IntegerBackedMemberGenerator(this, backend) {

        private String universeName() {
            return name + "Universe";
        }

        @Override
        public void generateFields(ValueBuilder valueBuilder) {
            FieldSpec universe = FieldSpec
                    .builder(ArrayTypeName.of(type), universeName())
                    .addModifiers(PRIVATE, STATIC, FINAL)
                    .initializer("$T.getUniverse($T.class)", Enums.class, type)
                    .build();
            valueBuilder.typeBuilder.addField(universe);
        }

        @Override
        protected void finishGet(MethodSpec.Builder methodBuilder, String value) {
            if (nullable()) {
                methodBuilder.addStatement("int ordinal = " + value);
                methodBuilder.addStatement(
                        format("return ordinal >= 0 ? %s[ordinal] : null", universeName()));
            } else {
                methodBuilder.addStatement(format("return %s[%s]", universeName(), value));
            }
        }

        @Override
        protected String startSet(MethodSpec.Builder methodBuilder) {
            if (nullable()) {
                return format("(%s != null ? %s.ordinal() : -1)", varName(), varName());
            } else {
                return varName() + ".ordinal()";
            }
        }
    };

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new ObjectHeapMemberGenerator(this);
    }
}
