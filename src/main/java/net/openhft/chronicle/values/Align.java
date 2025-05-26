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
 * Describes how a field should be aligned in the generated native layout. The field's
 * start offset is required to be a multiple of {@link #offset()} and its bytes must not
 * cross the boundary given by {@link #dontCross()}. Put this annotation on one accessor
 * of the field such as a getter or setter.
 * <p>
 * Alignment is measured from the beginning of the value instance. To keep the guarantee
 * when the instance is stored in native memory the instance itself should be aligned to
 * the coarsest field alignment.
 * <p>
 * The default alignment is determined by the field type, see {@link #DEFAULT}.
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface Align {
    /**
     * Rules used when explicit alignment is not provided:
     * <ul>
     * <li>For integer or floating point primitives {@link #dontCross()} defaults to a power
     * of two large enough for the size of the field, while {@link #offset()} is 1 byte.</li>
     * <li>For another value interface the {@link #offset()} default comes from
     * {@link ValueModel#recommendedOffsetAlignment()} and {@code dontCross} defaults to
     * {@link #NO_ALIGNMENT}.</li>
     * <li>For arrays the {@code offset} default is the maximum of element {@code offset} and
     * {@code dontCross} alignments, while {@code dontCross} defaults to {@link #NO_ALIGNMENT}.</li>
     * <li>For {@link CharSequence} fields {@code dontCross} defaults to {@link #NO_ALIGNMENT} and
     * {@code offset} has no default.</li>
     * <li>For {@code boolean} fields no default alignment applies.</li>
     * </ul>
     * Using {@code DEFAULT} defers to these rules. If a rule yields {@code NO_ALIGNMENT}, the
     * corresponding check is disabled.
     */
    int DEFAULT = -1;

    /**
     * No alignment constraints. When this value is used for {@link #offset()} or
     * {@link #dontCross()} the generator will not enforce alignment for the field.
     */
    int NO_ALIGNMENT = 0;

    /**
     * @return alignment in bytes for the field start. {@link #DEFAULT} applies the
     * type-specific rule which may resolve to {@link #NO_ALIGNMENT}.
     */
    int offset() default DEFAULT;

    /**
     * @return boundary in bytes that the field should not cross. {@link #DEFAULT}
     * delegates to the rule for the field type and may become {@link #NO_ALIGNMENT}.
     */
    int dontCross() default DEFAULT;
}
