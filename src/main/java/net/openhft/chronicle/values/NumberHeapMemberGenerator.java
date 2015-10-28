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
import sun.misc.Unsafe;

import static java.lang.String.format;

class NumberHeapMemberGenerator extends PrimitiveHeapMemberGenerator {

    NumberHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    private String getAndAdd() {
        return "getAndAdd" + capType;
    }

    @Override
    public void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(format("return %s($N += addition)", getCast()), field);
    }

    @Override
    public void generateArrayElementAdd(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(format("return %s($N[index] += addition)", getCast()), field);
    }

    @Override
    public void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement(
                format("return %s($N.$N(this, $N, addition) + addition)", getCast()),
                valueBuilder.unsafe(), getAndAdd(), fieldOffset(valueBuilder));
    }

    @Override
    public void generateArrayElementAddAtomic(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement(
                format("return %s($N.$N($N, (long) $T.$N + (index * (long) $T.$N), addition) + " +
                        "addition)", getCast()),
                valueBuilder.unsafe(), getAndAdd(), field, Unsafe.class, arrayBase(),
                Unsafe.class, arrayScale());
    }
}
