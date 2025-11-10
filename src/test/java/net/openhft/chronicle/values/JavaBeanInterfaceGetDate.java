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
