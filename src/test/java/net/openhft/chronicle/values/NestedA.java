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
 * First level nested value containing a text {@code key} and two
 * {@link NestedB} children named {@code one} and {@code two}.
 *
 * <p>Setter parameter names are:</p>
 * <ul>
 * <li>{@code key(String key)}</li>
 * <li>{@code one(NestedB one)}</li>
 * <li>{@code two(NestedB one)}</li>
 * </ul>
 */
public interface NestedA {
    void key(@MaxUtf8Length(64) String key);

    String key();

    void one(NestedB one);

    NestedB one();

    void two(NestedB one);

    NestedB two();
}
