/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;

/**
 * Marks a parameter as non-null. Value types are nullable by default
 * and this annotation expresses that a null argument is forbidden.
 * Validation code may inspect this annotation at run time.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({PARAMETER})
public @interface NotNull {
}
