/*
 * Copyright 2016-2021 chronicle.software
 *
 * https://chronicle.software
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

import java.util.Date;

import static java.lang.String.format;
import static net.openhft.chronicle.values.IntegerFieldModel.NORMAL_ACCESS_TYPE;

class DateFieldModel extends IntegerBackedFieldModel {

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
    void postProcess() {
        super.postProcess();
        backend.type = long.class;
        backend.range = RangeImpl.DEFAULT_LONG_RANGE;
        backend.postProcess();
    }

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
