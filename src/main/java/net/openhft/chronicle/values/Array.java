/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the properties of an array field: declared length and element
 * alignment. Index parameters in accessor methods are zero-based and operate in
 * the range {@code [0, length)}. The annotation <i>must</i> be placed on the
 * single accessor method which manipulates the array field, be that a getter,
 * setter or adder.
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
     * resolves to the element's own alignment when the element type is another
     * value interface; for primitive or reference elements it becomes
     * {@link Align#NO_ALIGNMENT}. Values less than {@code -1} are not allowed.
     */
    int elementOffsetAlignment() default Align.DEFAULT;

    /**
     * Specifies boundary which elements' bytes shouldn't cross, see {@link Align#dontCross()}.
     * Default is {@link Align#NO_ALIGNMENT}.
     */
    int elementDontCrossAlignment() default Align.NO_ALIGNMENT;
}
