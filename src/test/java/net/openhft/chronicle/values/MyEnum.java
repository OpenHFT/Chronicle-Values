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
 * Enumeration with numeric codes for verifying enum handling in unit tests.
 */
public enum MyEnum {
    /**
     * Constant {@code A} mapped to {@code 1}.
     */
    A(1),
    /**
     * Constant {@code B} mapped to {@code 2}.
     */
    B(2),
    /**
     * Constant {@code C} mapped to {@code 3}.
     */
    C(3);

    private final int var;

    MyEnum(int var) {
        this.var = var;
    }

    /**
     * Returns the numeric value associated with this constant. The tests
     * use this value as a custom encoding.
     */
    public int getVar() {
        return this.var;
    }
}
