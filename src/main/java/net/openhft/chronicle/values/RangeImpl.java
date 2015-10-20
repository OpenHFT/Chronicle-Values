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

import java.lang.annotation.Annotation;

final class RangeImpl implements Range {
    static final Range DEFAULT_BYTE_RANGE = new RangeImpl(Byte.MIN_VALUE, Byte.MAX_VALUE);
    static final Range DEFAULT_CHAR_RANGE = new RangeImpl(Character.MIN_VALUE, Character.MAX_VALUE);
    static final Range DEFAULT_SHORT_RANGE = new RangeImpl(Short.MIN_VALUE, Short.MAX_VALUE);
    static final Range DEFAULT_INT_RANGE = new RangeImpl(Integer.MIN_VALUE, Integer.MAX_VALUE);
    static final Range DEFAULT_LONG_RANGE = new RangeImpl(Long.MIN_VALUE, Long.MAX_VALUE);

    private final long min, max;

    RangeImpl(long min, long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public long min() {
        return min;
    }

    @Override
    public long max() {
        return max;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Range.class;
    }
}
