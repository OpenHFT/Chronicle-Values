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

class ObjectHeapMemberGenerator extends HeapMemberGenerator {
    ObjectHeapMemberGenerator(FieldModel fieldModel) {
        super(fieldModel);
    }

    @Override
    String putVolatile() {
        return "putVolatileObject";
    }

    @Override
    String putOrdered() {
        return "putOrderedObject";
    }

    @Override
    String compareAndSwap() {
        return "compareAndSwapObject";
    }

    @Override
    String arrayBase() {
        return "ARRAY_OBJECT_BASE_OFFSET";
    }

    @Override
    String arrayScale() {
        return "ARRAY_OBJECT_INDEX_SCALE";
    }

    @Override
    public void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("return ($T) $N.getObjectVolatile(this, $N)",
                fieldModel.type, valueBuilder.unsafe(), fieldOffset(valueBuilder));
    }

    @Override
    public void generateArrayElementGetVolatile(
            ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
            MethodSpec.Builder methodBuilder) {
        arrayFieldModel.checkBounds(methodBuilder);
        methodBuilder.addStatement("return ($T) $N.getObjectVolatile($N, " +
                "(long) $T.ARRAY_OBJECT_BASE_OFFSET + " +
                "(index * (long) $T.ARRAY_OBJECT_INDEX_SCALE))",
                fieldModel.type, valueBuilder.unsafe(), field, Unsafe.class, Unsafe.class);
    }
}
