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
 * Marker interface for objects that can copy state from another instance.
 * <p>
 * Generated value interfaces exist in both heap and native forms. Invoking
 * {@code copyFrom} allows an on-heap instance to copy the contents of an
 * off-heap reference or the other way round.
 */
@FunctionalInterface
public interface Copyable<T> {
    /**
     * Copies all fields from the provided instance into {@code this} instance.
     * Nested values are copied recursively via their own {@code copyFrom}
     * implementations. Implementations are not inherently thread-safe; callers
     * must ensure exclusive access to both instances during the operation.
     *
     * @param from source instance
     */
    void copyFrom(T from);
}
