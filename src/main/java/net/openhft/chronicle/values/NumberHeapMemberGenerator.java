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

class NumberHeapMemberGenerator extends PrimitiveBackedHeapMemberGenerator {

    NumberHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    private String getAndAdd() {
        return "getAndAdd" + capType;
    }

    @Override
    public void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        if (fieldModel.type != byte.class && fieldModel.type != char.class &&
                fieldModel.type != short.class) {
            methodBuilder.addStatement(
                    "$T $N = " + wrap(valueBuilder, methodBuilder, "$N") + " + addition",
                    fieldModel.type, fieldModel.varName(), field);
        } else {
            methodBuilder.addStatement(
                    "$T $N = ($T) (" + wrap(valueBuilder, methodBuilder, "$N") + " + addition)",
                    fieldModel.type, fieldModel.varName(), fieldModel.type, field);
        }
        methodBuilder.addStatement(
                "$N = " + unwrap(methodBuilder, "$N"), field, fieldModel.varName());
        methodBuilder.addStatement("return $N", fieldModel.varName());
    }

    @Override
    public void generateArrayElementAdd(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(
                "$T $N = " + wrap(valueBuilder, methodBuilder, "$N[index]") + " + addition",
                fieldModel.type, fieldModel.varName(), field);
        methodBuilder.addStatement("$N[index] = " + unwrap(methodBuilder, "$N"),
                field, fieldModel.varName());
        methodBuilder.addStatement("return $N", fieldModel.varName());
    }

    @Override
    public void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return " +
                        wrap(valueBuilder, methodBuilder, "$N.$N(this, $N, addition) + addition"),
                valueBuilder.unsafe(), getAndAdd(), fieldOffset(valueBuilder));
    }

    @Override
    public void generateArrayElementAddAtomic(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        Class type = Utils.UNSAFE_CLASS;
        methodBuilder.addStatement(
                "return " + wrap(valueBuilder, methodBuilder, "$N.$N($N, (long) $T.$N + " +
                        "(index * (long) $T.$N), addition) + addition"),
                valueBuilder.unsafe(), getAndAdd(), field, type, arrayBase(),
                type, arrayScale());
    }
}
