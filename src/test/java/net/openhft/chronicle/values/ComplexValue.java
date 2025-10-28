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

import net.openhft.chronicle.bytes.Byteable;

public interface ComplexValue extends Byteable, Copyable<ComplexValue> {

    enum Status {
        NEW,
        ACTIVE,
        CLOSED
    }

    @Group(0)
    long getId();

    @Group(0)
    void setId(long id);

    @Group(1)
    float getBalance();

    @Group(1)
    void setBalance(float balance);

    @Group(1)
    void setOrderedBalance(float balance);

    @Group(1)
    boolean compareAndSwapBalance(float expected, float update);

    @Group(1)
    float addBalance(float addition);

    @Group(2)
    Status getStatus();

    @Group(2)
    void setStatus(Status status);

    @Group(4)
    String getLabel();

    @Group(4)
    void setLabel(@MaxUtf8Length(12) String label);

    long getHistoryAt(int index);

    @Array(length = 3)
    void setHistoryAt(int index, long value);

    @Group(5)
    byte getCode();

    @Group(5)
    void setCode(byte code);

    @Group(5)
    boolean isEnabled();

    @Group(5)
    void setEnabled(boolean enabled);

    @Group(5)
    boolean isMirrorEnabled();

    @Group(5)
    void setMirrorEnabled(boolean enabled);
}
