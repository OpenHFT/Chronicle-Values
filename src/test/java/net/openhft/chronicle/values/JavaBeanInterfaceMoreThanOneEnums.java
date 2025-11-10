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
 * Test bean exposing several enums to ensure code generation copes with multiple
 * enumeration fields. Two members use {@link MyEnum} and another holds a
 * {@link BuySell} flag.
 */
public interface JavaBeanInterfaceMoreThanOneEnums {

    /**
     * Returns the first test enum.
     *
     * @return assigned constant
     */
    MyEnum getMyEnum1();

    /**
     * Updates the first test enum field.
     *
     * @param myEnum value to store
     */
    void setMyEnum1(MyEnum myEnum);

    /**
     * Returns the second test enum.
     *
     * @return assigned constant
     */
    MyEnum getMyEnum2();

    /**
     * Updates the second test enum field.
     *
     * @param myEnum value to store
     */
    void setMyEnum2(MyEnum myEnum);

    /**
     * Returns the trade side flag.
     *
     * @return BUY or SELL
     */
    BuySell getBuySell();

    /**
     * Updates the trade side flag.
     *
     * @param myEnum BUY or SELL value
     */
    void setBuySell(BuySell myEnum);
}
