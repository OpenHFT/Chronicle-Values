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
 * Changes the serialization order fields. Multiple fields can be in the same group and groups will
 * be ordered by their {@linkplain #value() values}, smallest first. This annotation should be put
 * on any single method accessing the field: getter, or setter, or adder, etc.
 * <p>
 * <p>If you don't provide a group for a field, it is considered to be a part of the "default
 * group", which is always the first group in the serialization order, regardless {@linkplain
 * #value() values} of other groups in the interface.
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface Group {
    /**
     * The value is used to order groups within the interface. Groups with smaller values are
     * serialized (and arranged in native implementation) before groups with bigger values.
     * <p>
     * <p>This field should be named "order", it is "value" to allow concise declaration form
     * like {@code @Group(1) int getFoo();}.
     *
     * @return the group ordering value
     */
    int value();
}
