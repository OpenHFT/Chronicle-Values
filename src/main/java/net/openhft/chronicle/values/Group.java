/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
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
