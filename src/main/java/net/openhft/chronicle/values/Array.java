/*
 * Copyright 2016-2025 chronicle.software
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
     * Number of elements in the array. Valid indexes are in the range
     * {@code [0, length)}. The length must be greater than one.
     */
    int length();

    /**
     * Specifies the alignment of element offsets, see {@link Align} for details.
     * A "more coarse" alignment means a larger power-of-two step and cannot
     * exceed the alignment of the array field itself. {@link Align#DEFAULT}
     * resolves to the element's own alignment when the element type is a value
     * interface, otherwise it becomes {@link Align#NO_ALIGNMENT}. Values less
     * than {@code -1} are not allowed.
     */
    int elementOffsetAlignment() default Align.DEFAULT;

    /**
     * Specifies boundary which elements' bytes shouldn't cross, see {@link Align#dontCross()}.
     * Default is {@link Align#NO_ALIGNMENT}.
     */
    int elementDontCrossAlignment() default Align.NO_ALIGNMENT;
}
