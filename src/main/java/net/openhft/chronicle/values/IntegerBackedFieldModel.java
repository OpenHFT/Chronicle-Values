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

import java.lang.reflect.Method;

class IntegerBackedFieldModel extends PrimitiveFieldModel {

    final IntegerFieldModel backend = new IntegerFieldModel(this);

    @Override
    void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        backend.addVolatileInfo(template);
    }

    @Override
    int sizeInBits() {
        return backend.sizeInBits();
    }

    @Override
    int offsetAlignmentInBytes() {
        return backend.offsetAlignmentInBytes();
    }

    @Override
    int dontCrossAlignmentInBytes() {
        return backend.dontCrossAlignmentInBytes();
    }

    @Override
    void checkState() {
        super.checkState();
        backend.checkState();
    }
}
