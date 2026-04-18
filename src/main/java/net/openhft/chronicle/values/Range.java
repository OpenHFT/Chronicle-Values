/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares that the annotated integer parameter must lie within a defined range.
 * <p>
 * Both {@link #min()} and {@link #max()} are inclusive. This is typically used
 * to validate array lengths, numeric options or memory offsets where a value
 * outside the range signals a bug.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@Documented
public @interface Range {
    /**
     * @return inclusive lower bound
     */
    long min() default Long.MIN_VALUE;

    /**
     * @return inclusive upper bound
     */
    long max() default Long.MAX_VALUE;
}
