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

import static net.openhft.chronicle.values.Nullability.NULLABLE;

public class EnumFieldModel extends IntegerBackedFieldModel {

    final FieldNullability nullability = new FieldNullability(this);

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        nullability.addTemplate(m, template);
    }

    @Override
    void postProcess() {
        super.postProcess();
        int min = nullability.nullability() == NULLABLE ? -1 : 0;
        int constants = Enums.numberOfConstants(type);
        if (constants == 0) {
            throw new IllegalStateException(
                    name + "field type is a enum with zero constants: " + type);
        }
        backend.range = new RangeImpl(min, constants - 1);
    }
}
