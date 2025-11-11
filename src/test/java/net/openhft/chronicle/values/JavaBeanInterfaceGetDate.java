/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.util.Date;

/**
 * Simple bean used in tests for {@code java.util.Date} handling.
 *
 * <p>
 * The date value is encoded as the number of milliseconds from the
 * epoch, so the generated implementation stores it as a {@code long}.
 * The setter accepts {@code null} because no
 * {@link net.openhft.chronicle.values.NotNull} annotation is present.
 * A {@code null} value indicates that no timestamp has been assigned.
 */
public interface JavaBeanInterfaceGetDate {

    /**
     * Returns the stored date or {@code null} if no value is set.
     */
    Date getDate();

    /**
     * Updates the stored date.
     *
     * @param date new value or {@code null} to clear the field
     */
    void setDate(Date date);
}
