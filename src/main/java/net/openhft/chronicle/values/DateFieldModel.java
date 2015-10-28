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

class DateFieldModel extends IntegerBackedFieldModel {

    @Override
    void postProcess() {
        super.postProcess();
        backend.type = long.class;
        backend.range = RangeImpl.DEFAULT_LONG_RANGE;
    }

    final MemberGenerator nativeGenerator = new IntegerBackedMemberGenerator(this, backend) {
        @Override
        protected void finishGet(MethodSpec.Builder methodBuilder, String value) {
            methodBuilder.addStatement(format("return new $T(%s)", value), Date.class);
        }

        @Override
        protected String startSet(MethodSpec.Builder methodBuilder) {
            return varName() + ".getTime()";
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
