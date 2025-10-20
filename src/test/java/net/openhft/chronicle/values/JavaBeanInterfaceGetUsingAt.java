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

import net.openhft.chronicle.core.values.LongValue;

/**
 * Interface exercising {@code getUsing} operations on a two element array of
 * {@link LongValue} instances.
 */
public interface JavaBeanInterfaceGetUsingAt {

    /**
     * Writes {@code using} into the slot identified by {@code index}.
     *
     * @param index zero-based array index, less than two
     * @param using value to be copied into the array
     */
    @Array(length = 2)
    void setItemAt(int index, LongValue using);

    /**
     * Copies the value stored at the given index into {@code using} to avoid
     * object creation.
     *
     * @param index zero-based array index, less than two
     * @param using container receiving the value
     * @return the passed in container for chaining
     */
    LongValue getUsingItemAt(int index, LongValue using);
}
