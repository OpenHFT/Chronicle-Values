//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

package net.openhft.chronicle.values;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines the fixed UTF-8 byte size for a CharSequence/String/StringBuilder field.
 * The {@code value()} method returns this maximum byte length.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@Documented
public @interface MaxUtf8Length {
    int value();
}
