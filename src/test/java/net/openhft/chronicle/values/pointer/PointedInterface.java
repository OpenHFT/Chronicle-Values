//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

package net.openhft.chronicle.values.pointer;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.values.MaxUtf8Length;

@SuppressWarnings("rawtypes")
/**
 * Target for pointer fields in {@link PointingInterface}. Holds text encoded in
 * UTF-8.
 */
public interface PointedInterface extends Byteable {

    String getString();

    /**
     * Stores text with a maximum length of twenty UTF-8 bytes.
     *
     * @param s text to store
     */
    void setString(@MaxUtf8Length(20) String s);
}
