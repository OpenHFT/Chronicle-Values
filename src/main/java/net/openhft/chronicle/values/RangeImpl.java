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

import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
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

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Range))
            return false;
        Range other = (Range) obj;
        return other.min() == min && other.max() == max;
    }
}
