/*
 *      Copyright (C) 2015, 2016  higherfrequencytrading.com
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

import java.lang.reflect.Method;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.*;
import static net.openhft.chronicle.values.IntegerFieldModel.NORMAL_ACCESS_TYPE;
import static net.openhft.chronicle.values.Nullability.NULLABLE;

class EnumFieldModel extends IntegerBackedFieldModel {

    final FieldNullability nullability = new FieldNullability(this);
    final MemberGenerator nativeGenerator = new IntegerBackedNativeMemberGenerator(this, backend) {

        @Override
        public void generateFields(ValueBuilder valueBuilder) {
            addUniverseField(valueBuilder);
        }

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            addUniverseField(valueBuilder);
        }

        @Override
        void finishGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String value) {
            methodBuilder.addStatement("return $N", fromOrdinalOrMinusOne(methodBuilder, value));
        }

        @Override
        String startSet(MethodSpec.Builder methodBuilder) {
            return toOrdinalOrMinusOne(varName());
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
            methodBuilder.addCode("if (($N) != other.$N()) return false;\n",
                    value, getOrGetVolatile().getName());
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genArrayElementGet(
                            arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE));
            methodBuilder.addCode("if (($N) != other.$N(index)) return false;\n",
                    value, arrayFieldModel.getOrGetVolatile().getName());
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE));
            return format("java.util.Objects.hashCode(%s)", value);
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String value = fromOrdinalOrMinusOne(methodBuilder,
                    backingFieldModel.genArrayElementGet(
                            arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE));
            return format("java.util.Objects.hashCode(%s)", value);
        }
    };

    @Override
    void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        nullability.addInfo(m, template);
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
        backend.postProcess();
    }

    private boolean nullable() {
        return nullability.nullability() == NULLABLE;
    }

    private String universeName() {
        return name + "Universe";
    }

    private void addUniverseField(ValueBuilder valueBuilder) {
        FieldSpec universe = FieldSpec
                .builder(ArrayTypeName.of(type), universeName())
                .addModifiers(PRIVATE, STATIC, FINAL)
                .initializer("$T.getUniverse($T.class)", Enums.class, type)
                .build();
        valueBuilder.typeBuilder.addField(universe);
    }

    private String toOrdinalOrMinusOne(String e) {
        if (nullable()) {
            return format("(%s != null ? %s.ordinal() : -1)", e, e);
        } else {
            return e + ".ordinal()";
        }
    }

    private String fromOrdinalOrMinusOne(MethodSpec.Builder methodBuilder, String value) {
        if (nullable()) {
            String ordinalVariableName = name() + "Ordinal";
            methodBuilder.addStatement(format("int %s = %s", ordinalVariableName, value));
            return format("%s >= 0 ? %s[%s] : null", ordinalVariableName, universeName(), ordinalVariableName);
        } else {
            return format("%s[%s]", universeName(), value);
        }
    }

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new ObjectHeapMemberGenerator(this) {

            @Override
            public void generateFields(ValueBuilder valueBuilder) {
                super.generateFields(valueBuilder);
                addUniverseField(valueBuilder);
            }

            @Override
            void generateArrayElementFields(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
                super.generateArrayElementFields(arrayFieldModel, valueBuilder);
                addUniverseField(valueBuilder);
            }

            @Override
            void generateWriteMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("bytes.writeStopBit($N)",
                        toOrdinalOrMinusOne(fieldName()));
            }

            @Override
            void generateArrayElementWriteMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("bytes.writeStopBit($N))",
                        toOrdinalOrMinusOne(fieldName() + "[index]"));
            }

            @Override
            void generateReadMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N = $N", fieldName(),
                        fromOrdinalOrMinusOne(methodBuilder, "(int) bytes.readStopBit()"));
            }

            @Override
            void generateArrayElementReadMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N[index] = $N", fieldName(),
                        fromOrdinalOrMinusOne(methodBuilder, "(int) bytes.readStopBit()"));
            }
        };
    }
}
