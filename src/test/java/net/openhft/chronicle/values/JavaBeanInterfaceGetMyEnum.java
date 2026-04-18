/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
