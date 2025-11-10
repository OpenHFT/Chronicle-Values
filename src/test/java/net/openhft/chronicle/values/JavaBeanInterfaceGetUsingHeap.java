//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

package net.openhft.chronicle.values;

/**
 * Interface used by tests verifying the {@code getUsing} pattern on a
 * heap backed implementation.
 */
public interface JavaBeanInterfaceGetUsingHeap {

    void setString(@MaxUtf8Length(8) String s);

    StringBuilder getUsingString(StringBuilder b);
}
