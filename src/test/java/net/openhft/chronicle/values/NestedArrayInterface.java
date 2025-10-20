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
 * Example interface used by the tests to exercise generation of nested arrays.
 * <p>
 * The {@code text} field is followed by two arrays: one of primitive ints and
 * one of {@link JavaBeanInterface} values. The {@link MaxUtf8Length} annotation
 * on {@link #setIntAt(int, int)} is deliberately superfluous; it normally
 * applies to character data and is included only to ensure such annotations are
 * ignored for primitive fields.
 */
public interface NestedArrayInterface {

    /** Returns the free-form text associated with this value. */
    String getText();

    /** Updates the text field. */
    void setText(String text);

    /**
     * Retrieves the integer stored at {@code index}.
     * The valid index range is defined by the test that uses this interface.
     */
    int getIntAt(int index);

    /**
     * Stores {@code value} at {@code index}. The {@code @MaxUtf8Length(16)}
     * annotation is ignored as the parameter is not character data.
     */
    void setIntAt(@MaxUtf8Length(16) int index, int value);

    /**
     * Returns the nested bean at {@code index}. The array contains
     * 32 elements as declared by {@code @Array(length = 32)}.
     */
    @Array(length = 32)
    JavaBeanInterface getJBIAt(int index);

    /**
     * Updates the nested bean stored at {@code index}.
     */
    void setJBIAt(int index, JavaBeanInterface jbi);
}
