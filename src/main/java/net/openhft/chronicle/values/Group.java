/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
 *
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
     *
     * <p>This field should be named "order", it is "value" to allow concise declaration form
     * like {@code @Group(1) int getFoo();}.
     *
     * @return the group ordering value
     */
    int value();
}
