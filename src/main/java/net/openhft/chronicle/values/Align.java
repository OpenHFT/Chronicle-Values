/*
 *      Copyright (C) 2015, 2016  higherfrequencytrading.com
 *      Copyright (C) 2016 Roman Leventov
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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the fields' arrangement in native implementation should be so that the offset to the
 * aligned field bytes from the beginning of the instance is a multiple of the specified {@link
 * #offset()}, the range of the field's bytes offsets don't cross the specified {@link #dontCross()}
 * boundary. This annotation should be put on any single method accessing the field: getter, or
 * setter, or adder, etc.
 * <p>
 * <p>This annotation guarantees alignment from the beginning of the instance, so to ensure
 * alignment in the native memory, the instance as a whole should be aligned by native memory
 * addresses to the most coarse alignment of it's fields.
 * <p>
 * <p>The default alignment depends on the field type, see {@link #DEFAULT}.
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface Align {
    /**
     * <ul>
     * <li>If the field type is an integer or a floating point primitive, default {@link
     * #dontCross()} alignment is equivalent to 1-byte for fields which take {@code <=} 8 bits, 2-byte
     * for fields which take {@code <=} 16 bits, 4-byte for fields which take {@code <=} 32 bits, 8-byte for
     * fields which take {@code <= } 64 bits. {@link #offset()} alignment is 1-byte for such fields.</li>
     * <li>If the field is another value interface, default {@code offset} alignment is
     * {@link ValueModel#recommendedOffsetAlignment()} for this sub-value interface. {@code
     * dontCross} default alignment for Value fields is {@link #NO_ALIGNMENT}.</li>
     * <li>If the field is an array, default {@code offset} alignment is equivalent to the
     * maximum of it's element {@code offset} or {@code dontCross} alignment, default {@code
     * dontCross} for array fields is {@code NO_ALIGNMENT}.
     * </li>
     * <li>If the field is of {@link CharSequence} type, default {@code dontCross} alignment
     * is {@link #NO_ALIGNMENT}. Default {@code offset} alignment is not applicable for
     * {@code CharSequence} fields, must be specified explicitly.</li>
     * <li>If the field is of {@code boolean} type, default alignment is not applicable, both
     * {@code offset} and {@code dontCross} alignment values must be specified explicitly.</li>
     * </ul>
     */
    int DEFAULT = -1;

    /**
     * 0 represents no particular alignment (shouldn't align the field start, shouldn't track
     * crossing of any border).
     */
    int NO_ALIGNMENT = 0;

    /**
     * The offset to the field's bytes should be a multiple of the {@code offset}.
     */
    int offset() default DEFAULT;

    /**
     * The field's bytes shouldn't cross the {@code dontCross} boundary.
     */
    int dontCross() default DEFAULT;
}
