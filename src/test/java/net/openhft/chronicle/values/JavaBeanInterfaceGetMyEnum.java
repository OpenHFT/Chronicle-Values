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
 * Accessors for storing a {@link MyEnum} value with Chronicle Values.
 *
 * <p>The generated implementation keeps the enumeration ordinal in the
 * backing {@code BytesStore} so that the value may be shared between
 * threads or processes.</p>
 */
public interface JavaBeanInterfaceGetMyEnum {

    /**
     * Reads the enumeration value from the underlying bytes.
     *
     * @return the currently stored {@link MyEnum}
     */
    MyEnum getMyEnum();

    /**
     * Writes the enumeration into the underlying bytes.
     *
     * @param myEnum the value to store, null is not permitted
     */
    void setMyEnum(MyEnum myEnum);
}
