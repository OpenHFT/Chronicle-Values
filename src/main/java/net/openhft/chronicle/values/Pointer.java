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
