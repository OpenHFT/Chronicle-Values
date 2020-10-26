/*
 *      Copyright (C) 2015, 2016-2020 chronicle.software
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
 * Specifies the properties of an array field: number of elements and their alignment. This
 * annotation <i>must</i> be put on a single method accessing the array elements: getter, or setter,
 * or adder, etc.
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface Array {
    /**
     * Specifies the array length, {@code index} in accessor methods should be between 0 and {@code
     * length - 1}, just like for vanilla Java arrays. This value should be positive.
     */
    int length();

    /**
     * Specifies the alignment of offsets of the elements, see {@link Align} for more information.
     * Elements' offsets alignment couldn't be more coarse than the offset alignment of the whole
     * array field. The {@link Align#DEFAULT} value specifies the alignment dependent on the element
     * type, if it is a Value generated interface, otherwise {@link Align#NO_ALIGNMENT}. Values less
     * than -1 are not allowed.
     */
    int elementOffsetAlignment() default Align.DEFAULT;

    /**
     * Specifies boundary which elements' bytes shouldn't cross, see {@link Align#dontCross()}.
     * Default is {@link Align#NO_ALIGNMENT}.
     */
    int elementDontCrossAlignment() default Align.NO_ALIGNMENT;
}
