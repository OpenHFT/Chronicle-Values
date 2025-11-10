//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

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
