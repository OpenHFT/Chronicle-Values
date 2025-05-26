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
 * Interface for getUsing behaviour on a simple bean.
 */
public interface JavaBeanInterfaceGetUsing {

    void setString(@MaxUtf8Length(8) String s);

    /**
     * Writes the stored value into the supplied builder.
     * <p>
     * Clears {@code b}, appends the last string set via
     * {@link #setString(String)}, and returns the builder.
     *
     * @param b receptacle for the value; overwritten each call
     * @return the provided builder for chaining
     */
    StringBuilder getUsingString(StringBuilder b);
}
