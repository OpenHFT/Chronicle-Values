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
