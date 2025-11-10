//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

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

/**
 * Demonstrates the {@code getUsing} pattern with {@link StringBuilder}.
 * <p>
 * Each getter copies the field value into the supplied buffer after clearing
 * it.  This allows callers to reuse a {@code StringBuilder} instance and so
 * avoid the allocation of a new {@code String} on every read.
 */
public interface GetUsingStringInterface {

    void setAnotherStringField(@MaxUtf8Length(64) String s);

    String getSomeStringField();

    void setSomeStringField(@MaxUtf8Length(64) @NotNull String s);

    /**
     * Copies {@code someStringField} into the given buffer after truncating it
     * to length zero.
     *
     * @param builder the buffer to reuse; must not be {@code null}
     */
    void getUsingSomeStringField(StringBuilder builder);

    /**
     * As {@link #getUsingSomeStringField(StringBuilder)} but returns the buffer
     * for convenience.
     *
     * @param builder the buffer to reuse; must not be {@code null}
     * @return the supplied {@code builder}
     */
    StringBuilder getUsingAnotherStringField(StringBuilder builder);
}
