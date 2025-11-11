/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks that the annotated parameter is a pointer to another value interface instance.
 * The stored value is the memory address rather than embedded bytes.
 * <p>Example usage:</p>
 * <pre>{@code
 * void link(@Pointer OtherValue v);
 * }</pre>
 * The generator stores v's address as a long.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@Documented
public @interface Pointer {
}
