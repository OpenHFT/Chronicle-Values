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

public class IntegerBackedFieldModel extends PrimitiveFieldModel {

    final IntegerFieldModel backend = new IntegerFieldModel();

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        backend.addTypeInfo(m, template);
        if (backend.range != null) {
            throw new IllegalStateException(name + " field couldn't have " +
                    backend.range + " annotation");
        }
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
}
