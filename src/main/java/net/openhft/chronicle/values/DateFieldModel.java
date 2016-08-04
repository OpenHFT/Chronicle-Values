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

import java.util.Date;

import static java.lang.String.format;
import static net.openhft.chronicle.values.IntegerFieldModel.NORMAL_ACCESS_TYPE;

class DateFieldModel extends IntegerBackedFieldModel {

    @Override
    void postProcess() {
        super.postProcess();
        backend.type = long.class;
        backend.range = RangeImpl.DEFAULT_LONG_RANGE;
        backend.postProcess();
    }

    final MemberGenerator nativeGenerator = new IntegerBackedNativeMemberGenerator(this, backend) {

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            // no fields
        }

        @Override
        void finishGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder, String value) {
            methodBuilder.addStatement(format("return new $T(%s)", value), Date.class);
        }

        @Override
        String startSet(MethodSpec.Builder methodBuilder) {
            return varName() + ".getTime()";
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String time = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            methodBuilder.addStatement("if ($N != other.$N().getTime()) return false;\n",
                    time, getOrGetVolatile().getName());
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String time = backingFieldModel.genArrayElementGet(
                    arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
            methodBuilder.addStatement("if ($N != other.$N(index).getTime()) return false;\n",
                    time, arrayFieldModel.getOrGetVolatile().getName());
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            String time = backingFieldModel.genGet(valueBuilder, NORMAL_ACCESS_TYPE);
            return format("java.lang.Long.hashCode(%s)", time);
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String time = backingFieldModel.genArrayElementGet(
                    arrayFieldModel, valueBuilder, methodBuilder, NORMAL_ACCESS_TYPE);
            return format("java.lang.Long.hashCode(%s)", time);
        }
    };

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new ObjectHeapMemberGenerator(this) {
            @Override
            void generateWriteMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("bytes.writeLong($N.getTime())", fieldName());
            }

            @Override
            void generateArrayElementWriteMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("bytes.writeLong($N[index].getTime())", fieldName());
            }

            @Override
            void generateReadMarshallable(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N = new Date(bytes.readLong())", fieldName());
            }

            @Override
            void generateArrayElementReadMarshallable(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addStatement("$N[index] = new Date(bytes.readLong())", fieldName());
            }
        };
    }
}
