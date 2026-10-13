/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * A value interface used only by {@link DumpGeneratedSourceTest}. It must not be
 * referenced by any other test so that its heap implementation is generated fresh
 * when that test triggers it, exercising the generated-source dump path.
 */
public interface DumpTestValue {
    int num();

    void num(int num);

    long size();

    void size(long size);
}
