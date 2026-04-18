/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
