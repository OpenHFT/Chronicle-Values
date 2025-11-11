/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesMarshallable;

/**
 * Minimal primitive field test interface.
 *
 * <p>Each `$` suffixed method operates on a primitive field and the unusual
 * name ensures code generators treat it as a raw field rather than a bean
 * accessor.
 */
@SuppressWarnings("rawtypes")
public interface MinimalInterface extends BytesMarshallable, Copyable<MinimalInterface>, Byteable {
    void flag(boolean flag);

    boolean flag();

    /**
     * Write the byte test field.
     *
     * @param b new value
     */
    void byte$(byte b);

    /**
     * Read the byte test field.
     */
    byte byte$();

    /**
     * Write the short test field.
     *
     * @param s new value
     */
    void short$(short s);

    /**
     * Read the short test field.
     */
    short short$();

    /**
     * Write the char test field.
     *
     * @param ch new value
     */
    void char$(char ch);

    /**
     * Read the char test field.
     */
    char char$();

    /**
     * Write the int test field.
     *
     * @param i new value
     */
    void int$(int i);

    /**
     * Read the int test field.
     */
    int int$();

    /**
     * Write the float test field.
     *
     * @param f new value
     */
    void float$(float f);

    /**
     * Read the float test field.
     */
    float float$();

    /**
     * Write the long test field.
     *
     * @param l new value
     */
    void long$(long l);

    /**
     * Read the long test field.
     */
    long long$();

    /**
     * Write the double test field.
     *
     * @param d new value
     */
    void double$(double d);

    /**
     * Read the double test field.
     */
    double double$();
}
