/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Controls field layout ordering by assigning groups. Multiple fields may share the same group and
 * groups are evaluated in ascending {@linkplain #value() order}. Apply this annotation to one of
 * the accessor methods (getter, setter, adder, etc.) for the field.
 * <p>
 * Fields without {@code @Group} belong to the implicit default group, which is always processed
 * before any explicit group regardless of the {@linkplain #value() values} used elsewhere in the
 * interface.
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface Group {
    /**
     * Determines the position of this group relative to others. Groups with equal values form a
     * single ordering slot and their fields are arranged internally using alignment heuristics.
     * The default group has an implicit order of {@code 0} and therefore precedes all explicit
     * groups, even those with negative values.
     * <p>
     * The property is named {@code value} to allow short syntax such as {@code @Group(1)}.
     *
     * @return group ordering value
     */
    int value();
}
